package com.temperature.proxy.infrastructure.adapter.out.openmeteo;

import com.temperature.proxy.domain.model.Coordinates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenMeteoClient {

    private static final String CURRENT_PARAMS = "temperature_2m,wind_speed_10m";

    private final RestClient openMeteoRestClient;

    public OpenMeteoResponse fetchCurrentWeather(Coordinates coordinates) {
        log.debug("Calling Open-Meteo API for lat={}, lon={}", coordinates.latitude(), coordinates.longitude());

        return openMeteoRestClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("latitude", coordinates.normalizedLatitude())
                        .queryParam("longitude", coordinates.normalizedLongitude())
                        .queryParam("current", CURRENT_PARAMS)
                        .build())
                .retrieve()
                .body(OpenMeteoResponse.class);
    }
}
