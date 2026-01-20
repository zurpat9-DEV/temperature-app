package com.temperature.proxy.edge;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
@DisplayName("Upstream Error Handling Tests")
class UpstreamErrorHandlingTest {

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
        registry.add("app.cache.ttl", () -> "1s");
    }

    @Nested
    @DisplayName("HTTP error responses")
    class HttpErrorResponses {

        @Test
        void should_return_502_when_upstream_returns_400() throws Exception {
            // given
            stubFor(WireMock.get(urlPathEqualTo("/v1/forecast"))
                    .willReturn(aResponse().withStatus(400).withBody("Bad Request")));

            // when/then
            mockMvc.perform(get("/api/v1/weather/current").param("lat", "10.10").param("lon", "20.20"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code", is("UPSTREAM_ERROR")));
        }

        @Test
        void should_return_502_when_upstream_returns_500() throws Exception {
            // given
            stubFor(WireMock.get(urlPathEqualTo("/v1/forecast"))
                    .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

            // when/then
            mockMvc.perform(get("/api/v1/weather/current").param("lat", "11.11").param("lon", "21.21"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code", is("UPSTREAM_ERROR")));
        }

        @Test
        void should_return_502_when_upstream_returns_503() throws Exception {
            // given
            stubFor(WireMock.get(urlPathEqualTo("/v1/forecast"))
                    .willReturn(aResponse().withStatus(503).withBody("Service Unavailable")));

            // when/then
            mockMvc.perform(get("/api/v1/weather/current").param("lat", "12.12").param("lon", "22.22"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code", is("UPSTREAM_ERROR")));
        }
    }

    @Nested
    @DisplayName("Timeout scenarios")
    class TimeoutScenarios {

        @Test
        void should_return_504_when_upstream_is_slow() throws Exception {
            // given
            stubFor(WireMock.get(urlPathEqualTo("/v1/forecast"))
                    .willReturn(aResponse().withStatus(200).withFixedDelay(2000)));

            // when/then
            mockMvc.perform(get("/api/v1/weather/current").param("lat", "13.13").param("lon", "23.23"))
                    .andExpect(status().isGatewayTimeout())
                    .andExpect(jsonPath("$.code", is("UPSTREAM_TIMEOUT")));
        }
    }

    @Nested
    @DisplayName("Malformed responses")
    class MalformedResponses {

        @Test
        void should_return_502_when_response_is_not_json() throws Exception {
            // given
            stubFor(WireMock.get(urlPathEqualTo("/v1/forecast"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", MediaType.TEXT_PLAIN_VALUE)
                            .withBody("Not JSON")));

            // when/then
            mockMvc.perform(get("/api/v1/weather/current").param("lat", "14.14").param("lon", "24.24"))
                    .andExpect(status().isBadGateway());
        }

        @Test
        void should_return_502_when_response_missing_current_field() throws Exception {
            // given
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
                                        "longitude": 13.41
                                    }
                                    """)));

            // when/then
            mockMvc.perform(get("/api/v1/weather/current").param("lat", "15.15").param("lon", "25.25"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code", is("UPSTREAM_INVALID_RESPONSE")));
        }
    }
}
