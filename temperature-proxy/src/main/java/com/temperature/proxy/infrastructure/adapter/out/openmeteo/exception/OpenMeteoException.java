package com.temperature.proxy.infrastructure.adapter.out.openmeteo.exception;

import lombok.Getter;

@Getter
public class OpenMeteoException extends RuntimeException {

    private final ErrorType errorType;

    private OpenMeteoException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }

    private OpenMeteoException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public static OpenMeteoException timeout(Throwable cause) {
        return new OpenMeteoException("Open-Meteo API timeout or connection error", cause, ErrorType.TIMEOUT);
    }

    public static OpenMeteoException clientError(Throwable cause) {
        return new OpenMeteoException("Open-Meteo API client error", cause, ErrorType.CLIENT_ERROR);
    }

    public static OpenMeteoException serverError(Throwable cause) {
        return new OpenMeteoException("Open-Meteo API server error", cause, ErrorType.SERVER_ERROR);
    }

    public static OpenMeteoException invalidResponse(String message) {
        return new OpenMeteoException("Invalid response from Open-Meteo API: " + message, ErrorType.INVALID_RESPONSE);
    }

    public static OpenMeteoException unexpected(Throwable cause) {
        return new OpenMeteoException("Unexpected error from Open-Meteo API", cause, ErrorType.UNEXPECTED);
    }

    public enum ErrorType {
        TIMEOUT,
        CLIENT_ERROR,
        SERVER_ERROR,
        INVALID_RESPONSE,
        UNEXPECTED
    }
}
