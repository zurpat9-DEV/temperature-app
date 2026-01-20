package com.temperature.proxy.application.service;

import com.temperature.proxy.domain.model.Coordinates;
import com.temperature.proxy.domain.model.WeatherData;
import com.temperature.proxy.domain.port.in.GetCurrentWeatherUseCase;
import com.temperature.proxy.domain.port.out.WeatherDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService implements GetCurrentWeatherUseCase {

    public static final String WEATHER_CACHE_NAME = "weather";

    private final WeatherDataProvider weatherDataProvider;

    @Override
    @Cacheable(value = WEATHER_CACHE_NAME, key = "#coordinates.toCacheKey()", sync = true)
    public WeatherData getCurrentWeather(Coordinates coordinates) {
        log.info(
                "Fetching weather data for coordinates: lat={}, lon={}",
                coordinates.latitude(),
                coordinates.longitude());
        return weatherDataProvider.fetchWeatherData(coordinates);
    }
}
