package com.temperature.proxy.infrastructure.adapter.in.web.exception;

import com.temperature.proxy.domain.exception.WeatherProviderException;
import com.temperature.proxy.infrastructure.adapter.in.web.dto.ApiError;
import com.temperature.proxy.infrastructure.adapter.in.web.dto.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ApiError.of(
                ErrorCode.INVALID_COORDINATES,
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        var message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Validation error");

        log.warn("Constraint violation: {}", message);
        return ApiError.of(
                ErrorCode.INVALID_COORDINATES, message, HttpStatus.BAD_REQUEST.value(), request.getRequestURI());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleMissingParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing parameter: {}", ex.getParameterName());
        return ApiError.of(
                ErrorCode.INVALID_COORDINATES,
                String.format("Parameter '%s' is required", ex.getParameterName()),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getValue());
        return ApiError.of(
                ErrorCode.INVALID_COORDINATES,
                String.format("Parameter '%s' must be a valid number", ex.getName()),
                HttpStatus.BAD_REQUEST.value(),
                request.getRequestURI());
    }

    @ExceptionHandler(WeatherProviderException.class)
    public ApiError handleWeatherProviderException(
            WeatherProviderException ex, HttpServletRequest request, HttpServletResponse response) {
        log.error("Weather provider error: {} - {}", ex.getErrorType(), ex.getMessage());

        var apiError =
                switch (ex.getErrorType()) {
                    case TIMEOUT -> {
                        response.setStatus(HttpStatus.GATEWAY_TIMEOUT.value());
                        yield ApiError.of(
                                ErrorCode.UPSTREAM_TIMEOUT,
                                ex.getMessage(),
                                HttpStatus.GATEWAY_TIMEOUT.value(),
                                request.getRequestURI());
                    }
                    case UNAVAILABLE -> {
                        response.setStatus(HttpStatus.BAD_GATEWAY.value());
                        yield ApiError.of(
                                ErrorCode.UPSTREAM_UNAVAILABLE,
                                ex.getMessage(),
                                HttpStatus.BAD_GATEWAY.value(),
                                request.getRequestURI());
                    }
                    case INVALID_RESPONSE -> {
                        response.setStatus(HttpStatus.BAD_GATEWAY.value());
                        yield ApiError.of(
                                ErrorCode.UPSTREAM_INVALID_RESPONSE,
                                ex.getMessage(),
                                HttpStatus.BAD_GATEWAY.value(),
                                request.getRequestURI());
                    }
                    case UPSTREAM_ERROR -> {
                        response.setStatus(HttpStatus.BAD_GATEWAY.value());
                        yield ApiError.of(
                                ErrorCode.UPSTREAM_ERROR,
                                ex.getMessage(),
                                HttpStatus.BAD_GATEWAY.value(),
                                request.getRequestURI());
                    }
                };
        return apiError;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ApiError handleRateLimitExceeded(RateLimitExceededException ex, HttpServletRequest request) {
        log.warn("Rate limit exceeded for IP: {}", request.getRemoteAddr());
        return ApiError.of(
                ErrorCode.RATE_LIMIT_EXCEEDED,
                "Too many requests. Please try again later.",
                HttpStatus.TOO_MANY_REQUESTS.value(),
                request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ApiError.of(
                ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                request.getRequestURI());
    }
}
