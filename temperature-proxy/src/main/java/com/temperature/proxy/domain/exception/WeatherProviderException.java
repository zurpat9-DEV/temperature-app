package com.temperature.proxy.domain.exception;

public class WeatherProviderException extends RuntimeException {

    private final ErrorType errorType;

    private WeatherProviderException(String message, Throwable cause, ErrorType errorType) {
        super(message, cause);
        this.errorType = errorType;
    }

    private WeatherProviderException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public static WeatherProviderException timeout(String message, Throwable cause) {
        return new WeatherProviderException(message, cause, ErrorType.TIMEOUT);
    }

    public static WeatherProviderException unavailable(String message, Throwable cause) {
        return new WeatherProviderException(message, cause, ErrorType.UNAVAILABLE);
    }

    public static WeatherProviderException invalidResponse(String message) {
        return new WeatherProviderException(message, ErrorType.INVALID_RESPONSE);
    }

    public static WeatherProviderException upstreamError(String message, Throwable cause) {
        return new WeatherProviderException(message, cause, ErrorType.UPSTREAM_ERROR);
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public enum ErrorType {
        TIMEOUT,
        UNAVAILABLE,
        INVALID_RESPONSE,
        UPSTREAM_ERROR
    }
}
