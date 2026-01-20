package com.temperature.proxy.domain.model;

public record Temperature(double celsius) {

    public Temperature {
        if (Double.isNaN(celsius) || Double.isInfinite(celsius)) {
            throw new IllegalArgumentException("Temperature must be a valid number");
        }
    }

    public static Temperature ofCelsius(double celsius) {
        return new Temperature(celsius);
    }
}
