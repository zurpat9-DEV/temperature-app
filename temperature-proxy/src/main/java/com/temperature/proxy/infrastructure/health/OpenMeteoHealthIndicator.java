package com.temperature.proxy.infrastructure.health;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component("openMeteo")
@RequiredArgsConstructor
public class OpenMeteoHealthIndicator implements HealthIndicator {

    private static final double TEST_LAT = 52.52;
    private static final double TEST_LON = 13.41;

    private final RestClient openMeteoRestClient;

    @Value("${app.open-meteo.timeout}")
    private Duration timeout;

    @Override
    public Health health() {
        try {
            var response = openMeteoRestClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("latitude", TEST_LAT)
                            .queryParam("longitude", TEST_LON)
                            .queryParam("current", "temperature_2m")
                            .build())
                    .retrieve()
                    .toBodilessEntity();

            if (response.getStatusCode().is2xxSuccessful()) {
                return Health.up()
                        .withDetail("service", "Open-Meteo API")
                        .withDetail("status", "reachable")
                        .build();
            } else {
                return Health.down()
                        .withDetail("service", "Open-Meteo API")
                        .withDetail("status", "error")
                        .withDetail("statusCode", response.getStatusCode().value())
                        .build();
            }
        } catch (Exception ex) {
            log.warn("Health check failed for Open-Meteo API: {}", ex.getMessage());
            return Health.down()
                    .withDetail("service", "Open-Meteo API")
                    .withDetail("status", "unreachable")
                    .withDetail("error", ex.getMessage())
                    .build();
        }
    }
}
