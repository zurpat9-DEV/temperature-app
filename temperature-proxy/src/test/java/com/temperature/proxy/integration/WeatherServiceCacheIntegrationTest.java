package com.temperature.proxy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.temperature.proxy.application.service.WeatherService;
import com.temperature.proxy.domain.model.Coordinates;
import com.temperature.proxy.domain.model.CurrentWeather;
import com.temperature.proxy.domain.model.Temperature;
import com.temperature.proxy.domain.model.WeatherData;
import com.temperature.proxy.domain.model.WindSpeed;
import com.temperature.proxy.domain.port.out.WeatherDataProvider;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@DisplayName("WeatherService with Cache Integration")
class WeatherServiceCacheIntegrationTest {

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private WeatherDataProvider weatherDataProvider;

    @AfterEach
    void clearCache() {
        var cache = cacheManager.getCache(WeatherService.WEATHER_CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
    }

    @Nested
    @DisplayName("Caching behavior")
    class CachingBehavior {

        @Test
        void should_cache_weather_data_for_same_coordinates() {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            var weatherData = createWeatherData(coordinates, 15.5, 10.2);
            given(weatherDataProvider.fetchWeatherData(coordinates)).willReturn(weatherData);

            // when
            var firstResult = weatherService.getCurrentWeather(coordinates);
            var secondResult = weatherService.getCurrentWeather(coordinates);
            var thirdResult = weatherService.getCurrentWeather(coordinates);

            // then
            assertThat(firstResult).isEqualTo(weatherData);
            assertThat(secondResult).isSameAs(firstResult);
            assertThat(thirdResult).isSameAs(firstResult);
            then(weatherDataProvider).should(times(1)).fetchWeatherData(coordinates);
        }

        @Test
        void should_use_separate_cache_entries_for_different_coordinates() {
            // given
            var coordinates1 = Coordinates.of(52.52, 13.41);
            var coordinates2 = Coordinates.of(48.85, 2.35);
            var weatherData1 = createWeatherData(coordinates1, 15.5, 10.2);
            var weatherData2 = createWeatherData(coordinates2, 20.0, 5.0);

            given(weatherDataProvider.fetchWeatherData(coordinates1)).willReturn(weatherData1);
            given(weatherDataProvider.fetchWeatherData(coordinates2)).willReturn(weatherData2);

            // when
            var result1a = weatherService.getCurrentWeather(coordinates1);
            var result2a = weatherService.getCurrentWeather(coordinates2);
            var result1b = weatherService.getCurrentWeather(coordinates1);
            var result2b = weatherService.getCurrentWeather(coordinates2);

            // then
            assertThat(result1a).isEqualTo(weatherData1);
            assertThat(result2a).isEqualTo(weatherData2);
            assertThat(result1b).isSameAs(result1a);
            assertThat(result2b).isSameAs(result2a);
            then(weatherDataProvider).should(times(1)).fetchWeatherData(coordinates1);
            then(weatherDataProvider).should(times(1)).fetchWeatherData(coordinates2);
        }

        @ParameterizedTest(name = "normalized lat={0}, lon={1}")
        @CsvSource({"52.52000, 13.41000", "52.52001, 13.41001", "52.5199999, 13.4099999"})
        void should_normalize_coordinates_for_cache_key(double latitude, double longitude) {
            // given
            var coordinates1 = Coordinates.of(52.52, 13.41);
            var coordinates2 = Coordinates.of(latitude, longitude);
            var weatherData = createWeatherData(coordinates1, 15.5, 10.2);
            given(weatherDataProvider.fetchWeatherData(coordinates1)).willReturn(weatherData);

            // when
            var result1 = weatherService.getCurrentWeather(coordinates1);
            var result2 = weatherService.getCurrentWeather(coordinates2);

            // then
            assertThat(result1).isEqualTo(weatherData);
            assertThat(result2).isSameAs(result1);
            then(weatherDataProvider).should(times(1)).fetchWeatherData(coordinates1);
        }
    }

    @Nested
    @DisplayName("Cache manager integration")
    class CacheManagerIntegration {

        @Test
        void should_store_entry_in_cache_manager() {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            var weatherData = createWeatherData(coordinates, 15.5, 10.2);
            given(weatherDataProvider.fetchWeatherData(coordinates)).willReturn(weatherData);

            // when
            weatherService.getCurrentWeather(coordinates);

            // then
            var cache = cacheManager.getCache(WeatherService.WEATHER_CACHE_NAME);
            assertThat(cache).isNotNull();
            var cachedValue = cache.get(coordinates.toCacheKey(), WeatherData.class);
            assertThat(cachedValue).isEqualTo(weatherData);
        }

        @Test
        void should_evict_entry_when_cache_cleared() {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);
            var weatherData = createWeatherData(coordinates, 15.5, 10.2);
            given(weatherDataProvider.fetchWeatherData(coordinates)).willReturn(weatherData);

            weatherService.getCurrentWeather(coordinates);
            var cache = cacheManager.getCache(WeatherService.WEATHER_CACHE_NAME);
            assertThat(cache).isNotNull();

            // when
            cache.clear();
            weatherService.getCurrentWeather(coordinates);

            // then
            then(weatherDataProvider).should(times(2)).fetchWeatherData(coordinates);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        void should_cache_data_at_coordinate_boundaries() {
            // given
            var coordinates = Coordinates.of(90.0, 180.0);
            var weatherData = createWeatherData(coordinates, -5.0, 0.0);
            given(weatherDataProvider.fetchWeatherData(coordinates)).willReturn(weatherData);

            // when
            var result1 = weatherService.getCurrentWeather(coordinates);
            var result2 = weatherService.getCurrentWeather(coordinates);

            // then
            assertThat(result1).isEqualTo(weatherData);
            assertThat(result2).isSameAs(result1);
            then(weatherDataProvider).should(times(1)).fetchWeatherData(coordinates);
        }

        @Test
        void should_cache_negative_coordinates() {
            // given
            var coordinates = Coordinates.of(-45.0, -90.0);
            var weatherData = createWeatherData(coordinates, 10.0, 20.0);
            given(weatherDataProvider.fetchWeatherData(coordinates)).willReturn(weatherData);

            // when
            var result1 = weatherService.getCurrentWeather(coordinates);
            var result2 = weatherService.getCurrentWeather(coordinates);

            // then
            assertThat(result1).isEqualTo(weatherData);
            assertThat(result2).isSameAs(result1);
            then(weatherDataProvider).should(times(1)).fetchWeatherData(coordinates);
        }
    }

    private WeatherData createWeatherData(Coordinates coordinates, double temperatureC, double windSpeedKmh) {
        var temperature = Temperature.ofCelsius(temperatureC);
        var windSpeed = WindSpeed.ofKmh(windSpeedKmh);
        var currentWeather = CurrentWeather.of(temperature, windSpeed);
        return WeatherData.of(coordinates, currentWeather, Instant.parse("2026-01-16T10:00:00Z"));
    }
}
