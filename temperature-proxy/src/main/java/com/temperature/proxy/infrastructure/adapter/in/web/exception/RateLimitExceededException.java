package com.temperature.proxy.infrastructure.adapter.in.web.exception;

public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}
