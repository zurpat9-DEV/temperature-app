package com.temperature.proxy.infrastructure.adapter.in.web;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.temperature.proxy.domain.exception.WeatherProviderException;
import com.temperature.proxy.domain.model.Coordinates;
import com.temperature.proxy.domain.model.CurrentWeather;
import com.temperature.proxy.domain.model.Temperature;
import com.temperature.proxy.domain.model.WeatherData;
import com.temperature.proxy.domain.model.WindSpeed;
import com.temperature.proxy.domain.port.in.GetCurrentWeatherUseCase;
import com.temperature.proxy.infrastructure.adapter.in.web.exception.GlobalExceptionHandler;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WeatherController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("WeatherController")
class WeatherControllerTest {

    private static final String WEATHER_ENDPOINT = "/api/v1/weather/current";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetCurrentWeatherUseCase getCurrentWeatherUseCase;

    @Nested
    @DisplayName("GET /api/v1/weather/current")
    class GetCurrentWeather {

        @Test
        void should_return_weather_data_when_valid_coordinates() throws Exception {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            var weatherData = createWeatherData(coordinates, 15.5, 10.2);
            given(getCurrentWeatherUseCase.getCurrentWeather(any(Coordinates.class)))
                    .willReturn(weatherData);

            // when/then
            mockMvc.perform(get(WEATHER_ENDPOINT).param("lat", "52.52").param("lon", "13.41"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.location.lat", is(52.52)))
                    .andExpect(jsonPath("$.location.lon", is(13.41)))
                    .andExpect(jsonPath("$.current.temperatureC", is(15.5)))
                    .andExpect(jsonPath("$.current.windSpeedKmh", is(10.2)))
                    .andExpect(jsonPath("$.source", is("open-meteo")));
        }

        @Test
        void should_return_400_when_lat_missing() throws Exception {
            // when/then
            mockMvc.perform(get(WEATHER_ENDPOINT).param("lon", "13.41"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("INVALID_COORDINATES")));
        }

        @Test
        void should_return_400_when_lon_missing() throws Exception {
            // when/then
            mockMvc.perform(get(WEATHER_ENDPOINT).param("lat", "52.52"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("INVALID_COORDINATES")));
        }

        @ParameterizedTest(name = "lat={0}")
        @CsvSource({"-91", "91", "-100", "100"})
        void should_return_400_when_latitude_out_of_range(String invalidLat) throws Exception {
            // when/then
            mockMvc.perform(get(WEATHER_ENDPOINT).param("lat", invalidLat).param("lon", "13.41"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("INVALID_COORDINATES")));
        }

        @ParameterizedTest(name = "lon={0}")
        @CsvSource({"-181", "181", "-200", "200"})
        void should_return_400_when_longitude_out_of_range(String invalidLon) throws Exception {
            // when/then
            mockMvc.perform(get(WEATHER_ENDPOINT).param("lat", "52.52").param("lon", invalidLon))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("INVALID_COORDINATES")));
        }

        @Test
        void should_return_400_when_lat_not_a_number() throws Exception {
            // when/then
            mockMvc.perform(get(WEATHER_ENDPOINT).param("lat", "abc").param("lon", "13.41"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code", is("INVALID_COORDINATES")));
        }

        @Test
        void should_return_504_when_upstream_timeout() throws Exception {
            // given
            given(getCurrentWeatherUseCase.getCurrentWeather(any(Coordinates.class)))
                    .willThrow(WeatherProviderException.timeout(
                            "Weather service did not respond in time", new RuntimeException("timeout")));

            // when/then
            mockMvc.perform(get(WEATHER_ENDPOINT).param("lat", "52.52").param("lon", "13.41"))
                    .andExpect(status().isGatewayTimeout())
                    .andExpect(jsonPath("$.code", is("UPSTREAM_TIMEOUT")));
        }

        @Test
        void should_return_502_when_upstream_unavailable() throws Exception {
            // given
            given(getCurrentWeatherUseCase.getCurrentWeather(any(Coordinates.class)))
                    .willThrow(WeatherProviderException.unavailable(
                            "Weather service is unavailable", new RuntimeException("server error")));

            // when/then
            mockMvc.perform(get(WEATHER_ENDPOINT).param("lat", "52.52").param("lon", "13.41"))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code", is("UPSTREAM_UNAVAILABLE")));
        }
    }

    private WeatherData createWeatherData(Coordinates coordinates, double temperature, double windSpeed) {
        var temp = Temperature.ofCelsius(temperature);
        var wind = WindSpeed.ofKmh(windSpeed);
        var currentWeather = CurrentWeather.of(temp, wind);
        return WeatherData.of(coordinates, currentWeather, Instant.parse("2026-01-11T10:12:54Z"));
    }
}
