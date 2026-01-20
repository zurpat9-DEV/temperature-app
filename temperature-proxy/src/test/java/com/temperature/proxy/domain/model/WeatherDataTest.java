package com.temperature.proxy.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WeatherData")
class WeatherDataTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        void should_create_weather_data_with_default_source_and_current_time() {
            var coordinates = Coordinates.of(52.52, 13.41);
            var currentWeather = createCurrentWeather();
            var beforeCreation = Instant.now();

            var weatherData = WeatherData.of(coordinates, currentWeather);

            var afterCreation = Instant.now();
            assertThat(weatherData.location()).isEqualTo(coordinates);
            assertThat(weatherData.currentWeather()).isEqualTo(currentWeather);
            assertThat(weatherData.source()).isEqualTo("open-meteo");
            assertThat(weatherData.retrievedAt()).isBetween(beforeCreation, afterCreation);
        }

        @Test
        void should_create_weather_data_with_custom_timestamp() {
            var coordinates = Coordinates.of(52.52, 13.41);
            var currentWeather = createCurrentWeather();
            var customTimestamp = Instant.parse("2024-01-15T12:00:00Z");

            var weatherData = WeatherData.of(coordinates, currentWeather, customTimestamp);

            assertThat(weatherData.location()).isEqualTo(coordinates);
            assertThat(weatherData.currentWeather()).isEqualTo(currentWeather);
            assertThat(weatherData.source()).isEqualTo("open-meteo");
            assertThat(weatherData.retrievedAt()).isEqualTo(customTimestamp);
        }

        @Test
        void should_create_weather_data_with_past_timestamp() {
            var coordinates = Coordinates.of(48.85, 2.35);
            var currentWeather = createCurrentWeather();
            var pastTimestamp = Instant.now().minus(1, ChronoUnit.HOURS);

            var weatherData = WeatherData.of(coordinates, currentWeather, pastTimestamp);

            assertThat(weatherData.retrievedAt()).isEqualTo(pastTimestamp);
        }

        @Test
        void should_create_weather_data_with_future_timestamp() {
            var coordinates = Coordinates.of(40.71, -74.01);
            var currentWeather = createCurrentWeather();
            var futureTimestamp = Instant.now().plus(1, ChronoUnit.HOURS);

            var weatherData = WeatherData.of(coordinates, currentWeather, futureTimestamp);

            assertThat(weatherData.retrievedAt()).isEqualTo(futureTimestamp);
        }

        @Test
        void should_create_using_record_constructor() {
            var coordinates = Coordinates.of(52.52, 13.41);
            var currentWeather = createCurrentWeather();
            var timestamp = Instant.now();
            var source = "test-source";

            var weatherData = new WeatherData(coordinates, currentWeather, source, timestamp);

            assertThat(weatherData.location()).isEqualTo(coordinates);
            assertThat(weatherData.currentWeather()).isEqualTo(currentWeather);
            assertThat(weatherData.source()).isEqualTo(source);
            assertThat(weatherData.retrievedAt()).isEqualTo(timestamp);
        }

        @Test
        void should_have_open_meteo_as_default_source() {
            var coordinates = Coordinates.of(52.52, 13.41);
            var currentWeather = createCurrentWeather();

            var weatherData = WeatherData.of(coordinates, currentWeather);

            assertThat(weatherData.source()).isEqualTo("open-meteo");
        }

        @Test
        void should_use_current_time_within_reasonable_bounds() {
            var coordinates = Coordinates.of(52.52, 13.41);
            var currentWeather = createCurrentWeather();

            var weatherData = WeatherData.of(coordinates, currentWeather);

            assertThat(weatherData.retrievedAt()).isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void should_accept_null_location_without_validation() {
            var currentWeather = createCurrentWeather();

            var weatherData = WeatherData.of(null, currentWeather);

            assertThat(weatherData.location()).isNull();
            assertThat(weatherData.currentWeather()).isEqualTo(currentWeather);
            assertThat(weatherData.source()).isEqualTo("open-meteo");
        }

        @Test
        void should_accept_null_current_weather_without_validation() {
            var coordinates = Coordinates.of(52.52, 13.41);

            var weatherData = WeatherData.of(coordinates, null);

            assertThat(weatherData.location()).isEqualTo(coordinates);
            assertThat(weatherData.currentWeather()).isNull();
            assertThat(weatherData.source()).isEqualTo("open-meteo");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        void should_be_equal_when_all_fields_match() {
            var coordinates = Coordinates.of(52.52, 13.41);
            var currentWeather = createCurrentWeather();
            var timestamp = Instant.parse("2024-01-15T12:00:00Z");
            var weather1 = WeatherData.of(coordinates, currentWeather, timestamp);
            var weather2 = WeatherData.of(coordinates, currentWeather, timestamp);

            assertThat(weather1).isEqualTo(weather2);
        }

        @Test
        void should_have_same_hashcode_when_equal() {
            var coordinates = Coordinates.of(52.52, 13.41);
            var currentWeather = createCurrentWeather();
            var timestamp = Instant.parse("2024-01-15T12:00:00Z");
            var weather1 = WeatherData.of(coordinates, currentWeather, timestamp);
            var weather2 = WeatherData.of(coordinates, currentWeather, timestamp);

            assertThat(weather1.hashCode()).isEqualTo(weather2.hashCode());
        }

        @Test
        void should_not_be_equal_when_coordinates_differ() {
            var currentWeather = createCurrentWeather();
            var timestamp = Instant.now();
            var weather1 = WeatherData.of(Coordinates.of(52.52, 13.41), currentWeather, timestamp);
            var weather2 = WeatherData.of(Coordinates.of(48.85, 2.35), currentWeather, timestamp);

            assertThat(weather1).isNotEqualTo(weather2);
        }

        @Test
        void should_not_be_equal_when_current_weather_differs() {
            var coordinates = Coordinates.of(52.52, 13.41);
            var timestamp = Instant.now();
            var weather1 = WeatherData.of(
                    coordinates, CurrentWeather.of(Temperature.ofCelsius(20.0), WindSpeed.ofKmh(10.0)), timestamp);
            var weather2 = WeatherData.of(
                    coordinates, CurrentWeather.of(Temperature.ofCelsius(25.0), WindSpeed.ofKmh(15.0)), timestamp);

            assertThat(weather1).isNotEqualTo(weather2);
        }

        @Test
        void should_not_be_equal_when_timestamp_differs() {
            var coordinates = Coordinates.of(52.52, 13.41);
            var currentWeather = createCurrentWeather();
            var weather1 = WeatherData.of(coordinates, currentWeather, Instant.parse("2024-01-15T12:00:00Z"));
            var weather2 = WeatherData.of(coordinates, currentWeather, Instant.parse("2024-01-15T13:00:00Z"));

            assertThat(weather1).isNotEqualTo(weather2);
        }

        @Test
        void should_be_equal_to_itself() {
            var weatherData = WeatherData.of(Coordinates.of(52.52, 13.41), createCurrentWeather());

            assertThat(weatherData).isEqualTo(weatherData);
        }

        @Test
        void should_not_be_equal_to_null() {
            var weatherData = WeatherData.of(Coordinates.of(52.52, 13.41), createCurrentWeather());

            assertThat(weatherData).isNotEqualTo(null);
        }
    }

    private CurrentWeather createCurrentWeather() {
        return CurrentWeather.of(Temperature.ofCelsius(20.5), WindSpeed.ofKmh(15.3));
    }
}
