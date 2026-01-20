package com.temperature.proxy.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current weather conditions")
public record CurrentConditionsDto(
        @Schema(description = "Temperature in Celsius", example = "1.2") double temperatureC,
        @Schema(description = "Wind speed in km/h", example = "9.7") double windSpeedKmh) {}
