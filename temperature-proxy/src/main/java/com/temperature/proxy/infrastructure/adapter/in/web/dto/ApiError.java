package com.temperature.proxy.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Error response")
public record ApiError(
        @Schema(description = "Error code", example = "INVALID_COORDINATES") String code,
        @Schema(description = "Error message", example = "Latitude must be between -90.0 and 90.0") String message,
        @Schema(description = "HTTP status code", example = "400") int status,
        @Schema(description = "Timestamp of the error", example = "2026-01-11T10:12:54Z") Instant timestamp,
        @Schema(description = "Request path", example = "/api/v1/weather/current") String path) {

    public static ApiError of(ErrorCode errorCode, String message, int status, String path) {
        return new ApiError(errorCode.name(), message, status, Instant.now(), path);
    }
}
