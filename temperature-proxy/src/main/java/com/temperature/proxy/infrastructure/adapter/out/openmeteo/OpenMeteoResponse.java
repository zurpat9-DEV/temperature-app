package com.temperature.proxy.infrastructure.adapter.out.openmeteo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenMeteoResponse(double latitude, double longitude, @JsonProperty("current") CurrentData current) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CurrentData(
            @JsonProperty("temperature_2m") double temperature2m,
            @JsonProperty("wind_speed_10m") double windSpeed10m) {}
}
