package com.temperature.proxy.infrastructure.adapter.in.web.dto;

public enum ErrorCode {
    INVALID_COORDINATES,
    RATE_LIMIT_EXCEEDED,
    UPSTREAM_TIMEOUT,
    UPSTREAM_UNAVAILABLE,
    UPSTREAM_ERROR,
    UPSTREAM_INVALID_RESPONSE,
    INTERNAL_ERROR
}
