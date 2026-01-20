package com.temperature.proxy.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.temperature.proxy.domain.model.Coordinates;
import com.temperature.proxy.domain.model.CurrentWeather;
import com.temperature.proxy.domain.model.Temperature;
import com.temperature.proxy.domain.model.WeatherData;
import com.temperature.proxy.domain.model.WindSpeed;
import com.temperature.proxy.domain.port.out.WeatherDataProvider;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherService")
class WeatherServiceTest {

    @Mock
    private WeatherDataProvider weatherDataProvider;

    private WeatherService weatherService;

    @BeforeEach
    void setUp() {
        weatherService = new WeatherService(weatherDataProvider);
    }

    @Test
    void should_return_weather_data_when_coordinates_valid() {
        // given
        var coordinates = Coordinates.of(52.52, 13.41);
        var expectedWeatherData = createWeatherData(coordinates);
        given(weatherDataProvider.fetchWeatherData(coordinates)).willReturn(expectedWeatherData);

        // when
        var result = weatherService.getCurrentWeather(coordinates);

        // then
        assertThat(result).isEqualTo(expectedWeatherData);
        then(weatherDataProvider).should().fetchWeatherData(coordinates);
    }

    @Test
    void should_delegate_to_weather_data_provider() {
        // given
        var coordinates = Coordinates.of(48.85, 2.35);
        var weatherData = createWeatherData(coordinates);
        given(weatherDataProvider.fetchWeatherData(coordinates)).willReturn(weatherData);

        // when
        weatherService.getCurrentWeather(coordinates);

        // then
        then(weatherDataProvider).should().fetchWeatherData(coordinates);
    }

    private WeatherData createWeatherData(Coordinates coordinates) {
        var temperature = Temperature.ofCelsius(15.5);
        var windSpeed = WindSpeed.ofKmh(10.2);
        var currentWeather = CurrentWeather.of(temperature, windSpeed);
        return WeatherData.of(coordinates, currentWeather, Instant.now());
    }
}
