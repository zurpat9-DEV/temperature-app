package com.temperature.proxy.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CurrentWeather")
class CurrentWeatherTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        void should_create_current_weather_when_all_fields_valid() {
            var temperature = Temperature.ofCelsius(20.5);
            var windSpeed = WindSpeed.ofKmh(15.3);

            var currentWeather = CurrentWeather.of(temperature, windSpeed);

            assertThat(currentWeather.temperature()).isEqualTo(temperature);
            assertThat(currentWeather.windSpeed()).isEqualTo(windSpeed);
        }

        @Test
        void should_create_current_weather_with_zero_temperature() {
            var temperature = Temperature.ofCelsius(0.0);
            var windSpeed = WindSpeed.ofKmh(10.0);

            var currentWeather = CurrentWeather.of(temperature, windSpeed);

            assertThat(currentWeather.temperature()).isEqualTo(temperature);
            assertThat(currentWeather.windSpeed()).isEqualTo(windSpeed);
        }

        @Test
        void should_create_current_weather_with_negative_temperature() {
            var temperature = Temperature.ofCelsius(-20.0);
            var windSpeed = WindSpeed.ofKmh(25.0);

            var currentWeather = CurrentWeather.of(temperature, windSpeed);

            assertThat(currentWeather.temperature()).isEqualTo(temperature);
        }

        @Test
        void should_create_current_weather_with_zero_wind_speed() {
            var temperature = Temperature.ofCelsius(20.0);
            var windSpeed = WindSpeed.ofKmh(0.0);

            var currentWeather = CurrentWeather.of(temperature, windSpeed);

            assertThat(currentWeather.windSpeed()).isEqualTo(windSpeed);
        }

        @Test
        void should_create_current_weather_with_high_values() {
            var temperature = Temperature.ofCelsius(50.0);
            var windSpeed = WindSpeed.ofKmh(200.0);

            var currentWeather = CurrentWeather.of(temperature, windSpeed);

            assertThat(currentWeather.temperature()).isEqualTo(temperature);
            assertThat(currentWeather.windSpeed()).isEqualTo(windSpeed);
        }

        @Test
        void should_create_using_record_constructor() {
            var temperature = Temperature.ofCelsius(20.5);
            var windSpeed = WindSpeed.ofKmh(15.3);

            var currentWeather = new CurrentWeather(temperature, windSpeed);

            assertThat(currentWeather.temperature()).isEqualTo(temperature);
            assertThat(currentWeather.windSpeed()).isEqualTo(windSpeed);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void should_accept_null_temperature_without_validation() {
            var windSpeed = WindSpeed.ofKmh(15.0);

            var currentWeather = CurrentWeather.of(null, windSpeed);

            assertThat(currentWeather.temperature()).isNull();
            assertThat(currentWeather.windSpeed()).isEqualTo(windSpeed);
        }

        @Test
        void should_accept_null_wind_speed_without_validation() {
            var temperature = Temperature.ofCelsius(20.0);

            var currentWeather = CurrentWeather.of(temperature, null);

            assertThat(currentWeather.temperature()).isEqualTo(temperature);
            assertThat(currentWeather.windSpeed()).isNull();
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        void should_be_equal_when_all_fields_match() {
            var temperature = Temperature.ofCelsius(20.5);
            var windSpeed = WindSpeed.ofKmh(15.3);
            var weather1 = CurrentWeather.of(temperature, windSpeed);
            var weather2 = CurrentWeather.of(temperature, windSpeed);

            assertThat(weather1).isEqualTo(weather2);
        }

        @Test
        void should_have_same_hashcode_when_equal() {
            var temperature = Temperature.ofCelsius(20.5);
            var windSpeed = WindSpeed.ofKmh(15.3);
            var weather1 = CurrentWeather.of(temperature, windSpeed);
            var weather2 = CurrentWeather.of(temperature, windSpeed);

            assertThat(weather1.hashCode()).isEqualTo(weather2.hashCode());
        }

        @Test
        void should_not_be_equal_when_temperature_differs() {
            var windSpeed = WindSpeed.ofKmh(15.3);
            var weather1 = CurrentWeather.of(Temperature.ofCelsius(20.5), windSpeed);
            var weather2 = CurrentWeather.of(Temperature.ofCelsius(20.6), windSpeed);

            assertThat(weather1).isNotEqualTo(weather2);
        }

        @Test
        void should_not_be_equal_when_wind_speed_differs() {
            var temperature = Temperature.ofCelsius(20.5);
            var weather1 = CurrentWeather.of(temperature, WindSpeed.ofKmh(15.3));
            var weather2 = CurrentWeather.of(temperature, WindSpeed.ofKmh(15.4));

            assertThat(weather1).isNotEqualTo(weather2);
        }

        @Test
        void should_be_equal_to_itself() {
            var currentWeather = CurrentWeather.of(Temperature.ofCelsius(20.5), WindSpeed.ofKmh(15.3));

            assertThat(currentWeather).isEqualTo(currentWeather);
        }

        @Test
        void should_not_be_equal_to_null() {
            var currentWeather = CurrentWeather.of(Temperature.ofCelsius(20.5), WindSpeed.ofKmh(15.3));

            assertThat(currentWeather).isNotEqualTo(null);
        }
    }
}
