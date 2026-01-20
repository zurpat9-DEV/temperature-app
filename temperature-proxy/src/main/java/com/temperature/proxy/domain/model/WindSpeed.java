package com.temperature.proxy.domain.model;

public record WindSpeed(double kmh) {

    public WindSpeed {
        if (Double.isNaN(kmh) || Double.isInfinite(kmh)) {
            throw new IllegalArgumentException("Wind speed must be a valid number");
        }
        if (kmh < 0) {
            throw new IllegalArgumentException("Wind speed cannot be negative");
        }
    }

    public static WindSpeed ofKmh(double kmh) {
        return new WindSpeed(kmh);
    }
}
