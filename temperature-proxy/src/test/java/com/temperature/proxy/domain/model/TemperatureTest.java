package com.temperature.proxy.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Temperature")
class TemperatureTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        void should_create_temperature_when_value_valid() {
            var celsius = 20.5;

            var temperature = Temperature.ofCelsius(celsius);

            assertThat(temperature.celsius()).isEqualTo(celsius);
        }

        @ParameterizedTest(name = "celsius={0}")
        @ValueSource(doubles = {-273.15, -100.0, -50.0, -10.0, 0.0, 10.0, 25.0, 37.5, 50.0, 100.0})
        void should_accept_valid_temperature_values(double celsius) {
            var temperature = Temperature.ofCelsius(celsius);

            assertThat(temperature).isNotNull();
            assertThat(temperature.celsius()).isEqualTo(celsius);
        }

        @Test
        void should_accept_absolute_zero() {
            var temperature = Temperature.ofCelsius(-273.15);

            assertThat(temperature.celsius()).isEqualTo(-273.15);
        }

        @Test
        void should_accept_extremely_high_temperature() {
            var temperature = Temperature.ofCelsius(1000.0);

            assertThat(temperature.celsius()).isEqualTo(1000.0);
        }

        @Test
        void should_accept_zero_temperature() {
            var temperature = Temperature.ofCelsius(0.0);

            assertThat(temperature.celsius()).isEqualTo(0.0);
        }

        @Test
        void should_accept_negative_temperature() {
            var temperature = Temperature.ofCelsius(-40.0);

            assertThat(temperature.celsius()).isEqualTo(-40.0);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        void should_throw_exception_when_temperature_is_nan() {
            assertThatThrownBy(() -> Temperature.ofCelsius(Double.NaN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Temperature must be a valid number");
        }

        @Test
        void should_throw_exception_when_temperature_is_positive_infinity() {
            assertThatThrownBy(() -> Temperature.ofCelsius(Double.POSITIVE_INFINITY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Temperature must be a valid number");
        }

        @Test
        void should_throw_exception_when_temperature_is_negative_infinity() {
            assertThatThrownBy(() -> Temperature.ofCelsius(Double.NEGATIVE_INFINITY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Temperature must be a valid number");
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        void should_be_equal_when_values_match() {
            var temp1 = Temperature.ofCelsius(20.5);
            var temp2 = Temperature.ofCelsius(20.5);

            assertThat(temp1).isEqualTo(temp2);
        }

        @Test
        void should_have_same_hashcode_when_equal() {
            var temp1 = Temperature.ofCelsius(20.5);
            var temp2 = Temperature.ofCelsius(20.5);

            assertThat(temp1.hashCode()).isEqualTo(temp2.hashCode());
        }

        @Test
        void should_not_be_equal_when_values_differ() {
            var temp1 = Temperature.ofCelsius(20.5);
            var temp2 = Temperature.ofCelsius(20.6);

            assertThat(temp1).isNotEqualTo(temp2);
        }

        @Test
        void should_be_equal_to_itself() {
            var temperature = Temperature.ofCelsius(20.5);

            assertThat(temperature).isEqualTo(temperature);
        }

        @Test
        void should_not_be_equal_to_null() {
            var temperature = Temperature.ofCelsius(20.5);

            assertThat(temperature).isNotEqualTo(null);
        }
    }
}
