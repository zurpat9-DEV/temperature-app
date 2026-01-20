package com.temperature.proxy.infrastructure.adapter.out.openmeteo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.temperature.proxy.domain.exception.WeatherProviderException;
import com.temperature.proxy.domain.model.Coordinates;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenMeteoWeatherAdapter")
class OpenMeteoWeatherAdapterTest {

    @Mock
    private OpenMeteoClient openMeteoClient;

    private OpenMeteoWeatherAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OpenMeteoWeatherAdapter(openMeteoClient, new SimpleMeterRegistry());
    }

    @Nested
    @DisplayName("Successful fetch")
    class SuccessfulFetch {

        @Test
        void should_return_weather_data_when_api_responds_successfully() {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            var response = new OpenMeteoResponse(52.52, 13.41, new OpenMeteoResponse.CurrentData(15.5, 10.2));
            given(openMeteoClient.fetchCurrentWeather(coordinates)).willReturn(response);

            // when
            var result = adapter.fetchWeatherData(coordinates);

            // then
            assertThat(result.location()).isEqualTo(coordinates);
            assertThat(result.currentWeather().temperature().celsius()).isEqualTo(15.5);
            assertThat(result.currentWeather().windSpeed().kmh()).isEqualTo(10.2);
            assertThat(result.source()).isEqualTo("open-meteo");
            assertThat(result.retrievedAt()).isNotNull();
        }

        @Test
        void should_preserve_negative_temperature() {
            // given
            var coordinates = Coordinates.of(60.0, 25.0);
            var response = new OpenMeteoResponse(60.0, 25.0, new OpenMeteoResponse.CurrentData(-15.3, 5.0));
            given(openMeteoClient.fetchCurrentWeather(coordinates)).willReturn(response);

            // when
            var result = adapter.fetchWeatherData(coordinates);

            // then
            assertThat(result.currentWeather().temperature().celsius()).isEqualTo(-15.3);
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        void should_throw_timeout_exception_when_resource_access_fails() {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            given(openMeteoClient.fetchCurrentWeather(coordinates))
                    .willThrow(new ResourceAccessException("Connection timeout"));

            // when/then
            assertThatThrownBy(() -> adapter.fetchWeatherData(coordinates))
                    .isInstanceOf(WeatherProviderException.class)
                    .satisfies(ex -> {
                        var providerEx = (WeatherProviderException) ex;
                        assertThat(providerEx.getErrorType()).isEqualTo(WeatherProviderException.ErrorType.TIMEOUT);
                    });
        }

        @Test
        void should_throw_unavailable_exception_when_http_5xx() {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            given(openMeteoClient.fetchCurrentWeather(coordinates))
                    .willThrow(HttpServerErrorException.create(
                            org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                            "Internal Server Error",
                            org.springframework.http.HttpHeaders.EMPTY,
                            new byte[0],
                            null));

            // when/then
            assertThatThrownBy(() -> adapter.fetchWeatherData(coordinates))
                    .isInstanceOf(WeatherProviderException.class)
                    .satisfies(ex -> {
                        var providerEx = (WeatherProviderException) ex;
                        assertThat(providerEx.getErrorType())
                                .isEqualTo(WeatherProviderException.ErrorType.UPSTREAM_ERROR);
                    });
        }

        @Test
        void should_throw_invalid_response_when_response_is_null() {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            given(openMeteoClient.fetchCurrentWeather(coordinates)).willReturn(null);

            // when/then
            assertThatThrownBy(() -> adapter.fetchWeatherData(coordinates))
                    .isInstanceOf(WeatherProviderException.class)
                    .satisfies(ex -> {
                        var providerEx = (WeatherProviderException) ex;
                        assertThat(providerEx.getErrorType())
                                .isEqualTo(WeatherProviderException.ErrorType.INVALID_RESPONSE);
                    });
        }

        @Test
        void should_throw_invalid_response_when_current_data_is_null() {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            var response = new OpenMeteoResponse(52.52, 13.41, null);
            given(openMeteoClient.fetchCurrentWeather(coordinates)).willReturn(response);

            // when/then
            assertThatThrownBy(() -> adapter.fetchWeatherData(coordinates))
                    .isInstanceOf(WeatherProviderException.class)
                    .satisfies(ex -> {
                        var providerEx = (WeatherProviderException) ex;
                        assertThat(providerEx.getErrorType())
                                .isEqualTo(WeatherProviderException.ErrorType.INVALID_RESPONSE);
                    });
        }
    }
}
