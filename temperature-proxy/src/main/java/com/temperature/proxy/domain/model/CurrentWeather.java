package com.temperature.proxy.domain.model;

public record CurrentWeather(Temperature temperature, WindSpeed windSpeed) {

    public static CurrentWeather of(Temperature temperature, WindSpeed windSpeed) {
        return new CurrentWeather(temperature, windSpeed);
    }
}
