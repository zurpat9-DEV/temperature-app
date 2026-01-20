package com.temperature.proxy.infrastructure.adapter.in.web;

import com.temperature.proxy.domain.model.Coordinates;
import com.temperature.proxy.domain.port.in.GetCurrentWeatherUseCase;
import com.temperature.proxy.infrastructure.adapter.in.web.dto.ApiError;
import com.temperature.proxy.infrastructure.adapter.in.web.dto.WeatherResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/weather")
@RequiredArgsConstructor
@Tag(name = "Weather", description = "Weather data API")
public class WeatherController {

    private final GetCurrentWeatherUseCase getCurrentWeatherUseCase;

    @Operation(
            summary = "Get current weather",
            description = "Fetches current temperature and wind speed for the specified coordinates")
    @ApiResponses(
            value = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Successfully retrieved weather data",
                        content = @Content(schema = @Schema(implementation = WeatherResponse.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Invalid coordinates",
                        content = @Content(schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(
                        responseCode = "429",
                        description = "Rate limit exceeded",
                        content = @Content(schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(
                        responseCode = "502",
                        description = "Upstream service error",
                        content = @Content(schema = @Schema(implementation = ApiError.class))),
                @ApiResponse(
                        responseCode = "504",
                        description = "Upstream service timeout",
                        content = @Content(schema = @Schema(implementation = ApiError.class)))
            })
    @GetMapping("/current")
    public WeatherResponse getCurrentWeather(
            @Parameter(description = "Latitude (-90 to 90)", example = "52.52")
                    @RequestParam("lat")
                    @NotNull(message = "Latitude is required")
                    @DecimalMin(value = "-90.0", message = "Latitude must be at least -90.0")
                    @DecimalMax(value = "90.0", message = "Latitude must be at most 90.0")
                    Double lat,
            @Parameter(description = "Longitude (-180 to 180)", example = "13.41")
                    @RequestParam("lon")
                    @NotNull(message = "Longitude is required")
                    @DecimalMin(value = "-180.0", message = "Longitude must be at least -180.0")
                    @DecimalMax(value = "180.0", message = "Longitude must be at most 180.0")
                    Double lon) {
        log.info("Received weather request for lat={}, lon={}", lat, lon);
        var coordinates = Coordinates.of(lat, lon);
        var weatherData = getCurrentWeatherUseCase.getCurrentWeather(coordinates);
        return WeatherResponse.fromDomain(weatherData);
    }
}
