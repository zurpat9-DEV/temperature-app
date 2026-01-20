package com.temperature.proxy.infrastructure.adapter.out.openmeteo;

import com.temperature.proxy.domain.exception.WeatherProviderException;
import com.temperature.proxy.domain.model.Coordinates;
import com.temperature.proxy.domain.model.CurrentWeather;
import com.temperature.proxy.domain.model.Temperature;
import com.temperature.proxy.domain.model.WeatherData;
import com.temperature.proxy.domain.model.WindSpeed;
import com.temperature.proxy.domain.port.out.WeatherDataProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

@Slf4j
@Component
public class OpenMeteoWeatherAdapter implements WeatherDataProvider {

    private static final String TIMER_NAME = "weather.upstream.latency";

    private final OpenMeteoClient openMeteoClient;
    private final Timer upstreamTimer;

    public OpenMeteoWeatherAdapter(OpenMeteoClient openMeteoClient, MeterRegistry meterRegistry) {
        this.openMeteoClient = openMeteoClient;
        this.upstreamTimer = Timer.builder(TIMER_NAME)
                .description("Open-Meteo API call duration")
                .register(meterRegistry);
    }

    @Override
    public WeatherData fetchWeatherData(Coordinates coordinates) {
        var retrievedAt = Instant.now();

        try {
            var response = upstreamTimer.record(() -> openMeteoClient.fetchCurrentWeather(coordinates));
            return mapToWeatherData(coordinates, response, retrievedAt);
        } catch (WeatherProviderException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.error("Timeout or connection error calling Open-Meteo API: {}", ex.getMessage());
            throw WeatherProviderException.timeout("Weather service did not respond in time", ex);
        } catch (HttpClientErrorException ex) {
            log.error("Client error from Open-Meteo API: {} - {}", ex.getStatusCode(), ex.getMessage());
            throw WeatherProviderException.upstreamError("Weather service returned an error", ex);
        } catch (HttpServerErrorException ex) {
            log.error("Server error from Open-Meteo API: {} - {}", ex.getStatusCode(), ex.getMessage());
            throw WeatherProviderException.upstreamError("Weather service returned an error", ex);
        } catch (Exception ex) {
            log.error("Unexpected error calling Open-Meteo API: {}", ex.getMessage());
            throw WeatherProviderException.unavailable("Weather service is unavailable", ex);
        }
    }

    private WeatherData mapToWeatherData(Coordinates coordinates, OpenMeteoResponse response, Instant retrievedAt) {
        if (response == null || response.current() == null) {
            throw WeatherProviderException.invalidResponse("Weather service returned invalid data");
        }

        var temperature = Temperature.ofCelsius(response.current().temperature2m());
        var windSpeed = WindSpeed.ofKmh(response.current().windSpeed10m());
        var currentWeather = CurrentWeather.of(temperature, windSpeed);

        return WeatherData.of(coordinates, currentWeather, retrievedAt);
    }
}
