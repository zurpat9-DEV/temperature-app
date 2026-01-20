package com.temperature.proxy.edge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.temperature.proxy.domain.model.Coordinates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Coordinate Edge Cases")
class CoordinateEdgeCaseTest {

    @Nested
    @DisplayName("Precision and normalization")
    class PrecisionAndNormalization {

        @ParameterizedTest(name = "lat={0}, expected normalized={1}")
        @CsvSource({"52.5, 52.50", "52.50, 52.50", "52.500, 52.50", "52.5000, 52.50", "-52.5, -52.50"})
        void should_normalize_trailing_zeros(double input, double expected) {
            // when
            var coordinates = Coordinates.of(input, 0.0);

            // then
            assertThat(coordinates.normalizedLatitude()).isEqualTo(expected);
        }

        @ParameterizedTest(name = "lat={0}, expected key starts with={1}")
        @CsvSource({
            "52.524999, 52.52",
            "52.525000, 52.53",
            "52.525001, 52.53",
            "-52.524999, -52.52",
            "-52.525000, -52.53"
        })
        void should_round_half_up(double input, String expectedPrefix) {
            // when
            var coordinates = Coordinates.of(input, 0.0);
            var cacheKey = coordinates.toCacheKey();

            // then
            assertThat(cacheKey).startsWith(expectedPrefix + ":");
        }

        @Test
        void should_handle_very_small_differences() {
            // given
            var coord1 = Coordinates.of(52.5249999999, 13.4149999999);
            var coord2 = Coordinates.of(52.5249999998, 13.4149999998);

            // then - should be equal due to rounding
            assertThat(coord1.toCacheKey()).isEqualTo(coord2.toCacheKey());
        }
    }

    @Nested
    @DisplayName("Boundary values")
    class BoundaryValues {

        @Test
        void should_accept_exact_boundary_latitude_negative() {
            // when
            var coordinates = Coordinates.of(-90.0, 0.0);

            // then
            assertThat(coordinates.latitude()).isEqualTo(-90.0);
        }

        @Test
        void should_accept_exact_boundary_latitude_positive() {
            // when
            var coordinates = Coordinates.of(90.0, 0.0);

            // then
            assertThat(coordinates.latitude()).isEqualTo(90.0);
        }

        @Test
        void should_accept_exact_boundary_longitude_negative() {
            // when
            var coordinates = Coordinates.of(0.0, -180.0);

            // then
            assertThat(coordinates.longitude()).isEqualTo(-180.0);
        }

        @Test
        void should_accept_exact_boundary_longitude_positive() {
            // when
            var coordinates = Coordinates.of(0.0, 180.0);

            // then
            assertThat(coordinates.longitude()).isEqualTo(180.0);
        }

        @Test
        void should_reject_just_over_boundary_latitude() {
            // when/then
            assertThatThrownBy(() -> Coordinates.of(90.001, 0.0)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_reject_just_under_boundary_latitude() {
            // when/then
            assertThatThrownBy(() -> Coordinates.of(-90.001, 0.0)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Special coordinate locations")
    class SpecialCoordinateLocations {

        @Test
        void should_handle_origin() {
            // when
            var coordinates = Coordinates.of(0.0, 0.0);

            // then
            assertThat(coordinates.toCacheKey()).isEqualTo("0.00:0.00");
        }

        @Test
        void should_handle_north_pole() {
            // when
            var coordinates = Coordinates.of(90.0, 0.0);

            // then
            assertThat(coordinates.toCacheKey()).isEqualTo("90.00:0.00");
        }

        @Test
        void should_handle_south_pole() {
            // when
            var coordinates = Coordinates.of(-90.0, 0.0);

            // then
            assertThat(coordinates.toCacheKey()).isEqualTo("-90.00:0.00");
        }

        @Test
        void should_handle_international_date_line_east() {
            // when
            var coordinates = Coordinates.of(0.0, 180.0);

            // then
            assertThat(coordinates.toCacheKey()).isEqualTo("0.00:180.00");
        }

        @Test
        void should_handle_international_date_line_west() {
            // when
            var coordinates = Coordinates.of(0.0, -180.0);

            // then
            assertThat(coordinates.toCacheKey()).isEqualTo("0.00:-180.00");
        }
    }

    @Nested
    @DisplayName("Invalid inputs")
    class InvalidInputs {

        @Test
        void should_reject_positive_infinity_latitude() {
            // when/then
            assertThatThrownBy(() -> Coordinates.of(Double.POSITIVE_INFINITY, 0.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_reject_negative_infinity_latitude() {
            // when/then
            assertThatThrownBy(() -> Coordinates.of(Double.NEGATIVE_INFINITY, 0.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_reject_nan_latitude() {
            // when/then
            assertThatThrownBy(() -> Coordinates.of(Double.NaN, 0.0)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_reject_positive_infinity_longitude() {
            // when/then
            assertThatThrownBy(() -> Coordinates.of(0.0, Double.POSITIVE_INFINITY))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_reject_negative_infinity_longitude() {
            // when/then
            assertThatThrownBy(() -> Coordinates.of(0.0, Double.NEGATIVE_INFINITY))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_reject_nan_longitude() {
            // when/then
            assertThatThrownBy(() -> Coordinates.of(0.0, Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
