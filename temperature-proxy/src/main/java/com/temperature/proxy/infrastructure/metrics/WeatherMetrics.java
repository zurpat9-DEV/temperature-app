package com.temperature.proxy.infrastructure.metrics;

import com.temperature.proxy.application.service.WeatherService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import lombok.Getter;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

@Component
@Getter
public class WeatherMetrics {

    private final Counter requestsTotal;
    private final Counter cacheHits;
    private final Counter cacheMisses;

    public WeatherMetrics(MeterRegistry registry, CacheManager cacheManager) {
        this.requestsTotal = Counter.builder("weather.requests.total")
                .description("Total weather requests")
                .register(registry);

        this.cacheHits = Counter.builder("weather.cache.hits")
                .description("Cache hit count")
                .register(registry);

        this.cacheMisses = Counter.builder("weather.cache.misses")
                .description("Cache miss count")
                .register(registry);

        var cache = cacheManager.getCache(WeatherService.WEATHER_CACHE_NAME);
        if (cache instanceof CaffeineCache caffeineCache) {
            CaffeineCacheMetrics.monitor(registry, caffeineCache.getNativeCache(), WeatherService.WEATHER_CACHE_NAME);
        }
    }

    public void incrementRequests() {
        requestsTotal.increment();
    }

    public void incrementCacheHits() {
        cacheHits.increment();
    }

    public void incrementCacheMisses() {
        cacheMisses.increment();
    }
}
