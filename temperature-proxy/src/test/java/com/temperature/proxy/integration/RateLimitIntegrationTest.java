package com.temperature.proxy.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Rate Limit Integration")
class RateLimitIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void startWireMock() {
        wireMockServer =
                new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        stubFor(
                WireMock.get(urlPathEqualTo("/v1/forecast"))
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
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.open-meteo.base-url", () -> wireMockServer.baseUrl() + "/v1/forecast");
        registry.add("app.rate-limit.requests-per-minute", () -> "5");
    }

    @Nested
    @DisplayName("Rate limiting behavior")
    class RateLimitingBehavior {

        @Test
        void should_allow_requests_within_limit() throws Exception {
            // when/then - 5 requests should succeed
            for (var i = 0; i < 5; i++) {
                var uniqueClient = "client-" + System.nanoTime() + "-" + i;
                mockMvc.perform(get("/api/v1/weather/current")
                                .param("lat", "52.52")
                                .param("lon", "13.41")
                                .header("X-Forwarded-For", uniqueClient))
                        .andExpect(status().isOk());
            }
        }

        @Test
        void should_reject_requests_exceeding_limit() throws Exception {
            // given
            var clientId = "client-" + System.nanoTime();

            // when - exhaust the limit (5 requests)
            for (var i = 0; i < 5; i++) {
                mockMvc.perform(get("/api/v1/weather/current")
                                .param("lat", "52.52")
                                .param("lon", "13.41")
                                .header("X-Forwarded-For", clientId))
                        .andExpect(status().isOk());
            }

            // then - 6th request should be rate limited
            mockMvc.perform(get("/api/v1/weather/current")
                            .param("lat", "52.52")
                            .param("lon", "13.41")
                            .header("X-Forwarded-For", clientId))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code", is("RATE_LIMIT_EXCEEDED")))
                    .andExpect(jsonPath("$.message", is("Too many requests. Please try again later.")))
                    .andExpect(header().string("Retry-After", "60"));
        }

        @Test
        void should_track_different_clients_separately() throws Exception {
            // given
            var client1 = "client1-" + System.nanoTime();
            var client2 = "client2-" + System.nanoTime();

            // when - client1 exhausts limit
            for (var i = 0; i < 5; i++) {
                mockMvc.perform(get("/api/v1/weather/current")
                                .param("lat", "52.52")
                                .param("lon", "13.41")
                                .header("X-Forwarded-For", client1))
                        .andExpect(status().isOk());
            }

            // then - client1 is rate limited
            mockMvc.perform(get("/api/v1/weather/current")
                            .param("lat", "52.52")
                            .param("lon", "13.41")
                            .header("X-Forwarded-For", client1))
                    .andExpect(status().isTooManyRequests());

            // then - client2 can still make requests
            mockMvc.perform(get("/api/v1/weather/current")
                            .param("lat", "52.52")
                            .param("lon", "13.41")
                            .header("X-Forwarded-For", client2))
                    .andExpect(status().isOk());
        }

        @Test
        void should_use_x_forwarded_for_header_when_present() throws Exception {
            // given
            var forwardedIp = "10.0.0." + System.nanoTime();

            // when - exhaust limit with X-Forwarded-For header
            for (var i = 0; i < 5; i++) {
                mockMvc.perform(get("/api/v1/weather/current")
                                .param("lat", "52.52")
                                .param("lon", "13.41")
                                .header("X-Forwarded-For", forwardedIp))
                        .andExpect(status().isOk());
            }

            // then - 6th request with same X-Forwarded-For is rate limited
            mockMvc.perform(get("/api/v1/weather/current")
                            .param("lat", "52.52")
                            .param("lon", "13.41")
                            .header("X-Forwarded-For", forwardedIp))
                    .andExpect(status().isTooManyRequests());
        }
    }

    @Nested
    @DisplayName("Excluded paths")
    class ExcludedPaths {

        @Test
        void should_not_rate_limit_actuator_health() throws Exception {
            // given
            var clientId = "test-client-" + System.nanoTime();

            // when/then - should allow more than limit (10 requests)
            for (var i = 0; i < 10; i++) {
                mockMvc.perform(get("/actuator/health").header("X-Forwarded-For", clientId))
                        .andExpect(status().isOk());
            }
        }

        @Test
        void should_not_rate_limit_api_docs() throws Exception {
            // given
            var clientId = "test-client-" + System.nanoTime();

            // when/then - should allow more than limit (10 requests)
            for (var i = 0; i < 10; i++) {
                mockMvc.perform(get("/v3/api-docs").header("X-Forwarded-For", clientId))
                        .andExpect(status().isOk());
            }
        }
    }

    @Nested
    @DisplayName("Response headers")
    class ResponseHeaders {

        @Test
        void should_include_retry_after_header_on_rate_limit() throws Exception {
            // given
            var clientId = "retry-test-" + System.nanoTime();

            // when - exhaust limit
            for (var i = 0; i < 5; i++) {
                mockMvc.perform(get("/api/v1/weather/current")
                                .param("lat", "52.52")
                                .param("lon", "13.41")
                                .header("X-Forwarded-For", clientId))
                        .andExpect(status().isOk());
            }

            // then - rate limited response has Retry-After header
            mockMvc.perform(get("/api/v1/weather/current")
                            .param("lat", "52.52")
                            .param("lon", "13.41")
                            .header("X-Forwarded-For", clientId))
                    .andExpect(header().exists("Retry-After"))
                    .andExpect(header().string("Retry-After", "60"));
        }
    }

    @Nested
    @DisplayName("Error response format")
    class ErrorResponseFormat {

        @Test
        void should_return_standardized_error_format() throws Exception {
            // given
            var clientId = "error-format-test-" + System.nanoTime();

            // when - exhaust limit
            for (var i = 0; i < 5; i++) {
                mockMvc.perform(get("/api/v1/weather/current")
                                .param("lat", "52.52")
                                .param("lon", "13.41")
                                .header("X-Forwarded-For", clientId))
                        .andExpect(status().isOk());
            }

            // then - error response has correct format
            mockMvc.perform(get("/api/v1/weather/current")
                            .param("lat", "52.52")
                            .param("lon", "13.41")
                            .header("X-Forwarded-For", clientId))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.path").exists())
                    .andExpect(jsonPath("$.status", is(429)))
                    .andExpect(jsonPath("$.path", is("/api/v1/weather/current")));
        }
    }
}
