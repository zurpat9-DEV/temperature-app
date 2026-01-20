package com.temperature.proxy.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.temperature.proxy.domain.exception.WeatherProviderException;
import com.temperature.proxy.domain.model.Coordinates;
import com.temperature.proxy.infrastructure.adapter.out.openmeteo.OpenMeteoWeatherAdapter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@DisplayName("OpenMeteoWeatherAdapter Integration")
class OpenMeteoAdapterIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private OpenMeteoWeatherAdapter openMeteoWeatherAdapter;

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

    @AfterEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.open-meteo.base-url", () -> wireMockServer.baseUrl() + "/v1/forecast");
    }

    @Nested
    @DisplayName("Successful weather data retrieval")
    class SuccessfulRetrieval {

        @Test
        void should_fetch_weather_data_with_valid_response() {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            stubFor(
                    get(urlPathEqualTo("/v1/forecast"))
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

            // when
            var result = openMeteoWeatherAdapter.fetchWeatherData(coordinates);

            // then
            assertThat(result).isNotNull();
            assertThat(result.location()).isEqualTo(coordinates);
            assertThat(result.currentWeather().temperature().celsius()).isEqualTo(15.5);
            assertThat(result.currentWeather().windSpeed().kmh()).isEqualTo(10.2);
            assertThat(result.retrievedAt()).isNotNull();
        }

        @ParameterizedTest(name = "temp={0}°C, wind={1} km/h")
        @CsvSource({"-40.0, 0.0", "0.0, 50.0", "35.5, 100.5", "-10.5, 25.3"})
        void should_handle_various_weather_values(double temperature, double windSpeed) {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            stubFor(get(urlPathEqualTo("/v1/forecast"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withBody(String.format(
                                    """
                                    {
                                        "latitude": 52.52,
                                        "longitude": 13.41,
                                        "current": {
                                            "temperature_2m": %.1f,
                                            "wind_speed_10m": %.1f
                                        }
                                    }
                                    """,
                                    temperature, windSpeed))));

            // when
            var result = openMeteoWeatherAdapter.fetchWeatherData(coordinates);

            // then
            assertThat(result.currentWeather().temperature().celsius()).isEqualTo(temperature);
            assertThat(result.currentWeather().windSpeed().kmh()).isEqualTo(windSpeed);
        }

        @Test
        void should_preserve_original_coordinates() {
            // given
            var coordinates = Coordinates.of(52.52345, 13.41987);
            stubFor(
                    get(urlPathEqualTo("/v1/forecast"))
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

            // when
            var result = openMeteoWeatherAdapter.fetchWeatherData(coordinates);

            // then
            assertThat(result.location()).isEqualTo(coordinates);
            assertThat(result.location().latitude()).isEqualTo(52.52345);
            assertThat(result.location().longitude()).isEqualTo(13.41987);
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        void should_throw_timeout_exception_when_upstream_delays() {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            stubFor(get(urlPathEqualTo("/v1/forecast"))
                    .willReturn(aResponse().withStatus(200).withFixedDelay(2000)));

            // when/then
            assertThatThrownBy(() -> openMeteoWeatherAdapter.fetchWeatherData(coordinates))
                    .isInstanceOf(WeatherProviderException.class)
                    .hasMessageContaining("did not respond in time");
        }

        @ParameterizedTest(name = "HTTP {0}")
        @CsvSource({"500", "502", "503"})
        void should_throw_unavailable_exception_for_server_errors(int statusCode) {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            stubFor(get(urlPathEqualTo("/v1/forecast")).willReturn(aResponse().withStatus(statusCode)));

            // when/then
            assertThatThrownBy(() -> openMeteoWeatherAdapter.fetchWeatherData(coordinates))
                    .isInstanceOf(WeatherProviderException.class)
                    .hasMessageContaining("returned an error");
        }

        @Test
        void should_throw_unavailable_exception_for_client_errors() {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            stubFor(get(urlPathEqualTo("/v1/forecast")).willReturn(aResponse().withStatus(400)));

            // when/then
            assertThatThrownBy(() -> openMeteoWeatherAdapter.fetchWeatherData(coordinates))
                    .isInstanceOf(WeatherProviderException.class)
                    .hasMessageContaining("returned an error");
        }

        @Test
        void should_throw_invalid_response_exception_when_current_missing() {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            stubFor(
                    get(urlPathEqualTo("/v1/forecast"))
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
            assertThatThrownBy(() -> openMeteoWeatherAdapter.fetchWeatherData(coordinates))
                    .isInstanceOf(WeatherProviderException.class)
                    .hasMessageContaining("invalid data");
        }

        @Test
        void should_throw_exception_when_response_null() {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            stubFor(get(urlPathEqualTo("/v1/forecast"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withBody("null")));

            // when/then
            assertThatThrownBy(() -> openMeteoWeatherAdapter.fetchWeatherData(coordinates))
                    .isInstanceOf(WeatherProviderException.class)
                    .hasMessageContaining("invalid data");
        }
    }

    @Nested
    @DisplayName("Coordinate edge cases")
    class CoordinateEdgeCases {

        @ParameterizedTest(name = "lat={0}, lon={1}")
        @CsvSource({"90.0, 180.0", "-90.0, -180.0", "0.0, 0.0", "45.5, -120.3"})
        void should_handle_boundary_coordinates(double latitude, double longitude) {
            // given
            var coordinates = Coordinates.of(latitude, longitude);
            stubFor(get(urlPathEqualTo("/v1/forecast"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withBody(String.format(
                                    """
                                    {
                                        "latitude": %.2f,
                                        "longitude": %.2f,
                                        "current": {
                                            "temperature_2m": 20.0,
                                            "wind_speed_10m": 10.0
                                        }
                                    }
                                    """,
                                    latitude, longitude))));

            // when
            var result = openMeteoWeatherAdapter.fetchWeatherData(coordinates);

            // then
            assertThat(result).isNotNull();
            assertThat(result.location().latitude()).isEqualTo(coordinates.normalizedLatitude());
            assertThat(result.location().longitude()).isEqualTo(coordinates.normalizedLongitude());
        }
    }

    @Nested
    @DisplayName("Response mapping")
    class ResponseMapping {

        @Test
        void should_map_all_response_fields_correctly() {
            // given
            var coordinates = Coordinates.of(48.85, 2.35);
            stubFor(
                    get(urlPathEqualTo("/v1/forecast"))
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
                                            "temperature_2m": 22.5,
                                            "wind_speed_10m": 15.8
                                        }
                                    }
                                    """)));

            // when
            var result = openMeteoWeatherAdapter.fetchWeatherData(coordinates);

            // then
            assertThat(result.location()).extracting("latitude", "longitude").containsExactly(48.85, 2.35);
            assertThat(result.currentWeather()).satisfies(weather -> {
                assertThat(weather.temperature().celsius()).isEqualTo(22.5);
                assertThat(weather.windSpeed().kmh()).isEqualTo(15.8);
            });
        }
    }
}
