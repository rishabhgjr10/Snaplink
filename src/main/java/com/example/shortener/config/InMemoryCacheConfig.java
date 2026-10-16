package com.example.shortener.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Fallback cache configuration that uses a simple in‑memory ConcurrentMapCacheManager
 * when Redis properties are not provided (e.g., during local dev with H2).
 */
@Configuration
@ConditionalOnProperty(name = "spring.redis.host", havingValue = "", matchIfMissing = true)
public class InMemoryCacheConfig {

    @Bean
    public CacheManager inMemoryCacheManager() {
        // "urlCache" is the cache name used by the application for short‑code → original URL.
        return new ConcurrentMapCacheManager("urlCache");
    }
}
