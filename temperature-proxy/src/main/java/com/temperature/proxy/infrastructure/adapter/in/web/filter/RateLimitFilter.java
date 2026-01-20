package com.temperature.proxy.infrastructure.adapter.in.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temperature.proxy.infrastructure.adapter.in.web.dto.ApiError;
import com.temperature.proxy.infrastructure.adapter.in.web.dto.ErrorCode;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String ACTUATOR_PATH = "/actuator";
    private static final String SWAGGER_PATH = "/swagger";
    private static final String API_DOCS_PATH = "/v3/api-docs";

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final int requestsPerMinute;

    public RateLimitFilter(
            ObjectMapper objectMapper, @Value("${app.rate-limit.requests-per-minute:100}") int requestsPerMinute) {
        this.objectMapper = objectMapper;
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        var requestPath = request.getRequestURI();
        if (isExcludedPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        var clientIp = getClientIp(request);
        var bucket = buckets.computeIfAbsent(clientIp, this::createBucket);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for client IP: {}", clientIp);
            sendRateLimitResponse(response, request.getRequestURI());
        }
    }

    private boolean isExcludedPath(String path) {
        return path.startsWith(ACTUATOR_PATH) || path.startsWith(SWAGGER_PATH) || path.startsWith(API_DOCS_PATH);
    }

    private String getClientIp(HttpServletRequest request) {
        var forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Bucket createBucket(String clientIp) {
        var limit = Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                .build();

        return Bucket.builder().addLimit(limit).build();
    }

    private void sendRateLimitResponse(HttpServletResponse response, String path) throws IOException {
        var error = ApiError.of(
                ErrorCode.RATE_LIMIT_EXCEEDED,
                "Too many requests. Please try again later.",
                HttpStatus.TOO_MANY_REQUESTS.value(),
                path);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", "60");
        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
