package com.temperature.proxy.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Location coordinates")
public record LocationDto(
        @Schema(description = "Latitude", example = "52.52") double lat,
        @Schema(description = "Longitude", example = "13.41") double lon) {}
