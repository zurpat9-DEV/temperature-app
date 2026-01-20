package com.temperature.proxy.domain.model;

import java.time.Instant;

public record WeatherData(Coordinates location, CurrentWeather currentWeather, String source, Instant retrievedAt) {

    private static final String DEFAULT_SOURCE = "open-meteo";

    public static WeatherData of(Coordinates location, CurrentWeather currentWeather) {
        return new WeatherData(location, currentWeather, DEFAULT_SOURCE, Instant.now());
    }

    public static WeatherData of(Coordinates location, CurrentWeather currentWeather, Instant retrievedAt) {
        return new WeatherData(location, currentWeather, DEFAULT_SOURCE, retrievedAt);
    }
}
