package com.temperature.proxy.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Coordinates")
class CoordinatesTest {

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        void should_create_valid_coordinates_when_values_in_range() {
            // given
            var lat = 52.52;
            var lon = 13.41;

            // when
            var coordinates = Coordinates.of(lat, lon);

            // then
            assertThat(coordinates.latitude()).isEqualTo(lat);
            assertThat(coordinates.longitude()).isEqualTo(lon);
        }

        @ParameterizedTest(name = "lat={0}, lon={1}")
        @CsvSource({"-90.0, 0.0", "90.0, 0.0", "0.0, -180.0", "0.0, 180.0", "-90.0, -180.0", "90.0, 180.0"})
        void should_accept_boundary_values(double lat, double lon) {
            // when
            var coordinates = Coordinates.of(lat, lon);

            // then
            assertThat(coordinates).isNotNull();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @ParameterizedTest(name = "latitude={0}")
        @ValueSource(doubles = {-90.01, -91.0, -180.0, 90.01, 91.0, 180.0})
        void should_throw_exception_when_latitude_out_of_range(double invalidLat) {
            // when/then
            assertThatThrownBy(() -> Coordinates.of(invalidLat, 0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Latitude");
        }

        @ParameterizedTest(name = "longitude={0}")
        @ValueSource(doubles = {-180.01, -181.0, -270.0, 180.01, 181.0, 270.0})
        void should_throw_exception_when_longitude_out_of_range(double invalidLon) {
            // when/then
            assertThatThrownBy(() -> Coordinates.of(0.0, invalidLon))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Longitude");
        }

        @Test
        void should_throw_exception_when_latitude_is_nan() {
            // when/then
            assertThatThrownBy(() -> Coordinates.of(Double.NaN, 0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("valid number");
        }

        @Test
        void should_throw_exception_when_longitude_is_nan() {
            // when/then
            assertThatThrownBy(() -> Coordinates.of(0.0, Double.NaN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("valid number");
        }

        @Test
        void should_throw_exception_when_latitude_is_infinite() {
            // when/then
            assertThatThrownBy(() -> Coordinates.of(Double.POSITIVE_INFINITY, 0.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void should_throw_exception_when_longitude_is_infinite() {
            // when/then
            assertThatThrownBy(() -> Coordinates.of(0.0, Double.NEGATIVE_INFINITY))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Cache Key")
    class CacheKey {

        @Test
        void should_generate_cache_key_with_two_decimal_places() {
            // given
            var coordinates = Coordinates.of(52.52, 13.41);

            // when
            var cacheKey = coordinates.toCacheKey();

            // then
            assertThat(cacheKey).isEqualTo("52.52:13.41");
        }

        @ParameterizedTest(name = "input={0},{1} -> key={2}")
        @CsvSource({
            "52.5, 13.4, 52.50:13.40",
            "52.524, 13.415, 52.52:13.42",
            "52.525, 13.415, 52.53:13.42",
            "-52.524, -13.415, -52.52:-13.42"
        })
        void should_normalize_coordinates_for_cache_key(double lat, double lon, String expectedKey) {
            // given
            var coordinates = Coordinates.of(lat, lon);

            // when
            var cacheKey = coordinates.toCacheKey();

            // then
            assertThat(cacheKey).isEqualTo(expectedKey);
        }

        @Test
        void should_return_normalized_latitude() {
            // given
            var coordinates = Coordinates.of(52.5234567, 13.41);

            // when
            var normalized = coordinates.normalizedLatitude();

            // then
            assertThat(normalized).isEqualTo(52.52);
        }

        @Test
        void should_return_normalized_longitude() {
            // given
            var coordinates = Coordinates.of(52.52, 13.4156789);

            // when
            var normalized = coordinates.normalizedLongitude();

            // then
            assertThat(normalized).isEqualTo(13.42);
        }
    }

    @Nested
    @DisplayName("Equality")
    class Equality {

        @Test
        void should_be_equal_when_normalized_values_match() {
            // given
            var coord1 = Coordinates.of(52.521, 13.411);
            var coord2 = Coordinates.of(52.524, 13.414);

            // when/then
            assertThat(coord1).isEqualTo(coord2);
        }

        @Test
        void should_have_same_hashcode_when_equal() {
            // given
            var coord1 = Coordinates.of(52.521, 13.411);
            var coord2 = Coordinates.of(52.524, 13.414);

            // when/then
            assertThat(coord1.hashCode()).isEqualTo(coord2.hashCode());
        }

        @Test
        void should_not_be_equal_when_normalized_values_differ() {
            // given
            var coord1 = Coordinates.of(52.52, 13.41);
            var coord2 = Coordinates.of(52.53, 13.42);

            // when/then
            assertThat(coord1).isNotEqualTo(coord2);
        }
    }
}
