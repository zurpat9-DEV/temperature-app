package com.temperature.proxy.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
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
@DisplayName("Weather API Integration Tests")
class WeatherApiIntegrationTest {

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
    void resetCache() {
        wireMockServer.resetAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.open-meteo.base-url", () -> wireMockServer.baseUrl() + "/v1/forecast");
    }

    @Nested
    @DisplayName("End-to-end weather retrieval")
    class EndToEndWeatherRetrieval {

        @Test
        void should_return_weather_data_from_open_meteo() throws Exception {
            // given
            stubFor(
                    com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/v1/forecast"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
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

            // when/then
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                    "/api/v1/weather/current")
                            .param("lat", "52.52")
                            .param("lon", "13.41"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.location.lat", is(52.52)))
                    .andExpect(jsonPath("$.location.lon", is(13.41)))
                    .andExpect(jsonPath("$.current.temperatureC", is(15.5)))
                    .andExpect(jsonPath("$.current.windSpeedKmh", is(10.2)))
                    .andExpect(jsonPath("$.source", is("open-meteo")))
                    .andExpect(jsonPath("$.retrievedAt", notNullValue()));
        }

        @Test
        void should_return_504_when_upstream_times_out() throws Exception {
            // given
            stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/v1/forecast"))
                    .willReturn(aResponse().withStatus(200).withFixedDelay(2000)));

            // when/then
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                    "/api/v1/weather/current")
                            .param("lat", "52.52")
                            .param("lon", "13.41"))
                    .andExpect(status().isGatewayTimeout())
                    .andExpect(jsonPath("$.code", is("UPSTREAM_TIMEOUT")));
        }

        @Test
        void should_return_502_when_upstream_returns_500() throws Exception {
            // given
            stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/v1/forecast"))
                    .willReturn(aResponse().withStatus(500)));

            // when/then
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                    "/api/v1/weather/current")
                            .param("lat", "52.52")
                            .param("lon", "13.41"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code", is("UPSTREAM_ERROR")));
        }
    }

    @Nested
    @DisplayName("Caching behavior")
    class CachingBehavior {

        @Test
        void should_cache_response_for_same_coordinates() throws Exception {
            // given
            wireMockServer.resetAll();
            stubFor(
                    com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/v1/forecast"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                            .withBody(
                                                    """
                                    {
                                        "latitude": 48.85,
                                        "longitude": 2.35,
                                        "current": {
                                            "temperature_2m": 20.0,
                                            "wind_speed_10m": 5.0
                                        }
                                    }
                                    """)));

            // when - first request
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                    "/api/v1/weather/current")
                            .param("lat", "48.85")
                            .param("lon", "2.35"))
                    .andExpect(status().isOk());

            // when - second request (should be cached)
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                    "/api/v1/weather/current")
                            .param("lat", "48.85")
                            .param("lon", "2.35"))
                    .andExpect(status().isOk());

            // then - only one request to upstream
            wireMockServer.verify(
                    1, com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor(urlPathEqualTo("/v1/forecast")));
        }
    }
}
