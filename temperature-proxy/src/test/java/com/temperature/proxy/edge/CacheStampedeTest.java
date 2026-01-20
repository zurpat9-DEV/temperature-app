package com.temperature.proxy.edge;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Cache Stampede Protection Tests")
class CacheStampedeTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    @BeforeAll
    static void startWireMock() {
        wireMockServer =
                new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.open-meteo.base-url", () -> wireMockServer.baseUrl() + "/v1/forecast");
        registry.add("app.cache.ttl", () -> "60s");
        registry.add("app.cache.refresh-after-write", () -> "50s");
    }

    @Nested
    @DisplayName("Concurrent requests same coordinates")
    class ConcurrentRequestsSameCoordinates {

        @Test
        void should_make_single_upstream_call_for_concurrent_requests() throws Exception {
            // given
            stubOpenMeteoResponse(200);

            var threadCount = 10;
            var startLatch = new CountDownLatch(1);
            var finishLatch = new CountDownLatch(threadCount);
            var successCount = new AtomicInteger(0);
            var lat = "30.00";
            var lon = "40.00";

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // when
            IntStream.range(0, threadCount)
                    .forEach(i -> executor.submit(() -> {
                        try {
                            startLatch.await();
                            var result = mockMvc.perform(get("/api/v1/weather/current")
                                            .param("lat", lat)
                                            .param("lon", lon))
                                    .andReturn();

                            if (result.getResponse().getStatus() == 200) {
                                successCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            finishLatch.countDown();
                        }
                    }));

            startLatch.countDown();
            finishLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();

            // then
            assertThat(successCount.get()).isEqualTo(threadCount);
            verify(1, getRequestedFor(urlPathEqualTo("/v1/forecast")));
        }

        @Test
        void should_serve_from_cache_after_initial_load() throws Exception {
            // given
            stubOpenMeteoResponse(100);
            var lat = "31.00";
            var lon = "41.00";

            // when - first request populates cache
            mockMvc.perform(get("/api/v1/weather/current").param("lat", lat).param("lon", lon))
                    .andExpect(status().isOk());

            // when - subsequent requests should hit cache
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(get("/api/v1/weather/current").param("lat", lat).param("lon", lon))
                        .andExpect(status().isOk());
            }

            // then - only one upstream call
            verify(1, getRequestedFor(urlPathEqualTo("/v1/forecast")));
        }
    }

    @Nested
    @DisplayName("Different coordinates")
    class DifferentCoordinates {

        @Test
        void should_make_separate_calls_for_different_coordinates() throws Exception {
            // given
            stubOpenMeteoResponse(0);

            List<String[]> coordinates = List.of(
                    new String[] {"32.00", "42.00"}, new String[] {"33.00", "43.00"}, new String[] {"34.00", "44.00"});

            // when
            for (String[] coord : coordinates) {
                mockMvc.perform(get("/api/v1/weather/current")
                                .param("lat", coord[0])
                                .param("lon", coord[1]))
                        .andExpect(status().isOk());
            }

            // then
            verify(3, getRequestedFor(urlPathEqualTo("/v1/forecast")));
        }
    }

    @Nested
    @DisplayName("Cache key normalization")
    class CacheKeyNormalization {

        @Test
        void should_use_same_cache_entry_for_coordinates_with_same_precision() throws Exception {
            // given
            stubOpenMeteoResponse(0);

            // when - coordinates that normalize to same cache key
            mockMvc.perform(get("/api/v1/weather/current")
                            .param("lat", "35.123456")
                            .param("lon", "45.987654"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/weather/current").param("lat", "35.12").param("lon", "45.99"))
                    .andExpect(status().isOk());

            // then - single call due to cache key rounding
            verify(1, getRequestedFor(urlPathEqualTo("/v1/forecast")));
        }

        @Test
        void should_use_different_cache_entry_for_significantly_different_coordinates() throws Exception {
            // given
            stubOpenMeteoResponse(0);

            // when - coordinates that normalize to different cache keys
            mockMvc.perform(get("/api/v1/weather/current").param("lat", "36.10").param("lon", "46.10"))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/weather/current").param("lat", "36.20").param("lon", "46.20"))
                    .andExpect(status().isOk());

            // then - two calls due to different cache keys
            verify(2, getRequestedFor(urlPathEqualTo("/v1/forecast")));
        }
    }

    private void stubOpenMeteoResponse(int delayMs) {
        stubFor(
                WireMock.get(urlPathEqualTo("/v1/forecast"))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withFixedDelay(delayMs)
                                        .withBody(
                                                """
                                {
                                    "latitude": 52.52,
                                    "longitude": 13.41,
                                    "current": {
                                        "temperature_2m": 15.5,
                                        "wind_speed_10m": 10.2
                                    }
                                }
                                """)));
    }
}
