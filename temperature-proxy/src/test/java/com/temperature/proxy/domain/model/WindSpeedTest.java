package com.temperature.proxy.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("WindSpeed")
class WindSpeedTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        void should_create_wind_speed_when_value_valid() {
            var kmh = 15.5;

            var windSpeed = WindSpeed.ofKmh(kmh);

            assertThat(windSpeed.kmh()).isEqualTo(kmh);
        }

        @ParameterizedTest(name = "kmh={0}")
        @ValueSource(doubles = {0.0, 0.1, 5.0, 10.5, 25.0, 50.0, 100.0, 150.0, 200.0, 300.0})
        void should_accept_valid_wind_speed_values(double kmh) {
            var windSpeed = WindSpeed.ofKmh(kmh);

            assertThat(windSpeed).isNotNull();
            assertThat(windSpeed.kmh()).isEqualTo(kmh);
        }

        @Test
        void should_accept_zero_wind_speed() {
            var windSpeed = WindSpeed.ofKmh(0.0);

            assertThat(windSpeed.kmh()).isEqualTo(0.0);
        }

        @Test
        void should_accept_very_small_positive_wind_speed() {
            var windSpeed = WindSpeed.ofKmh(0.001);

            assertThat(windSpeed.kmh()).isEqualTo(0.001);
        }

        @Test
        void should_accept_extremely_high_wind_speed() {
            var windSpeed = WindSpeed.ofKmh(500.0);

            assertThat(windSpeed.kmh()).isEqualTo(500.0);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void should_throw_exception_when_wind_speed_is_negative() {
            assertThatThrownBy(() -> WindSpeed.ofKmh(-1.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Wind speed cannot be negative");
        }

        @ParameterizedTest(name = "kmh={0}")
        @ValueSource(doubles = {-0.1, -1.0, -10.0, -100.0, -500.0})
        void should_throw_exception_for_negative_values(double kmh) {
            assertThatThrownBy(() -> WindSpeed.ofKmh(kmh))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Wind speed cannot be negative");
        }

        @Test
        void should_throw_exception_when_wind_speed_is_nan() {
            assertThatThrownBy(() -> WindSpeed.ofKmh(Double.NaN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Wind speed must be a valid number");
        }

        @Test
        void should_throw_exception_when_wind_speed_is_positive_infinity() {
            assertThatThrownBy(() -> WindSpeed.ofKmh(Double.POSITIVE_INFINITY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Wind speed must be a valid number");
        }

        @Test
        void should_throw_exception_when_wind_speed_is_negative_infinity() {
            assertThatThrownBy(() -> WindSpeed.ofKmh(Double.NEGATIVE_INFINITY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Wind speed must be a valid number");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        void should_be_equal_when_values_match() {
            var wind1 = WindSpeed.ofKmh(15.5);
            var wind2 = WindSpeed.ofKmh(15.5);

            assertThat(wind1).isEqualTo(wind2);
        }

        @Test
        void should_have_same_hashcode_when_equal() {
            var wind1 = WindSpeed.ofKmh(15.5);
            var wind2 = WindSpeed.ofKmh(15.5);

            assertThat(wind1.hashCode()).isEqualTo(wind2.hashCode());
        }

        @Test
        void should_not_be_equal_when_values_differ() {
            var wind1 = WindSpeed.ofKmh(15.5);
            var wind2 = WindSpeed.ofKmh(15.6);

            assertThat(wind1).isNotEqualTo(wind2);
        }

        @Test
        void should_be_equal_to_itself() {
            var windSpeed = WindSpeed.ofKmh(15.5);

            assertThat(windSpeed).isEqualTo(windSpeed);
        }

        @Test
        void should_not_be_equal_to_null() {
            var windSpeed = WindSpeed.ofKmh(15.5);

            assertThat(windSpeed).isNotEqualTo(null);
        }
    }
}
