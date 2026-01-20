package com.temperature.proxy.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Coordinates(double latitude, double longitude) {

    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;
    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;
    private static final int CACHE_KEY_PRECISION = 2;

    public Coordinates {
        validateLatitude(latitude);
        validateLongitude(longitude);
    }

    public static Coordinates of(double latitude, double longitude) {
        return new Coordinates(latitude, longitude);
    }

    public String toCacheKey() {
        return formatCoordinate(latitude) + ":" + formatCoordinate(longitude);
    }

    public double normalizedLatitude() {
        return roundToScale(latitude, CACHE_KEY_PRECISION);
    }

    public double normalizedLongitude() {
        return roundToScale(longitude, CACHE_KEY_PRECISION);
    }

    private static void validateLatitude(double latitude) {
        if (Double.isNaN(latitude) || Double.isInfinite(latitude)) {
            throw new IllegalArgumentException("Latitude must be a valid number");
        }
        if (latitude < MIN_LATITUDE || latitude > MAX_LATITUDE) {
            throw new IllegalArgumentException(String.format(
                    "Latitude must be between %.1f and %.1f, got: %.6f", MIN_LATITUDE, MAX_LATITUDE, latitude));
        }
    }

    private static void validateLongitude(double longitude) {
        if (Double.isNaN(longitude) || Double.isInfinite(longitude)) {
            throw new IllegalArgumentException("Longitude must be a valid number");
        }
        if (longitude < MIN_LONGITUDE || longitude > MAX_LONGITUDE) {
            throw new IllegalArgumentException(String.format(
                    "Longitude must be between %.1f and %.1f, got: %.6f", MIN_LONGITUDE, MAX_LONGITUDE, longitude));
        }
    }

    private static String formatCoordinate(double value) {
        return String.format("%.2f", roundToScale(value, CACHE_KEY_PRECISION));
    }

    private static double roundToScale(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Coordinates that = (Coordinates) obj;
        return Objects.equals(toCacheKey(), that.toCacheKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(toCacheKey());
    }
}
