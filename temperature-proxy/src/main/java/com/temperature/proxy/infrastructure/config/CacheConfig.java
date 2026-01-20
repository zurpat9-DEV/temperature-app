package com.temperature.proxy.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.temperature.proxy.application.service.WeatherService;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
            @Value("${app.cache.ttl}") Duration ttl, @Value("${app.cache.max-size}") int maxSize) {
        var caffeineBuilder =
                Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(maxSize).recordStats();

        var cacheManager = new CaffeineCacheManager(WeatherService.WEATHER_CACHE_NAME);
        cacheManager.setCaffeine(caffeineBuilder);
        return cacheManager;
    }
}
