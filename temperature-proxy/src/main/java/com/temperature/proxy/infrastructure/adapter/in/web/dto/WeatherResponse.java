package com.temperature.proxy.infrastructure.adapter.in.web.dto;

import com.temperature.proxy.domain.model.WeatherData;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Weather response with current conditions")
public record WeatherResponse(
        @Schema(description = "Location coordinates") LocationDto location,
        @Schema(description = "Current weather conditions") CurrentConditionsDto current,
        @Schema(description = "Data source", example = "open-meteo") String source,
        @Schema(description = "Timestamp when data was retrieved", example = "2026-01-11T10:12:54Z")
                Instant retrievedAt) {

    public static WeatherResponse fromDomain(WeatherData weatherData) {
        var location = new LocationDto(
                weatherData.location().normalizedLatitude(),
                weatherData.location().normalizedLongitude());

        var current = new CurrentConditionsDto(
                weatherData.currentWeather().temperature().celsius(),
                weatherData.currentWeather().windSpeed().kmh());

        return new WeatherResponse(location, current, weatherData.source(), weatherData.retrievedAt());
    }
}
