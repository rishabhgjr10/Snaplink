package com.example.shortener.service;

import com.example.shortener.config.AppProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Short code to destination URL cache.
 * <ul>
 *   <li>Redis when configured, otherwise a bounded in-memory Caffeine cache.</li>
 *   <li>Every entry has a TTL, so a stale entry cannot live forever.</li>
 *   <li>Writes and evictions inside a transaction run only after it commits,
 *       so a rolled-back change never reaches the cache.</li>
 *   <li>After a Redis failure, Redis is skipped for {@link #BREAKER_OPEN} instead of
 *       making every redirect wait for a connection timeout.</li>
 * </ul>
 */
@Component
public class UrlCache {

    private static final Logger log = LoggerFactory.getLogger(UrlCache.class);
    private static final String KEY_PREFIX = "url:";
    static final Duration BREAKER_OPEN = Duration.ofSeconds(30);

    private final StringRedisTemplate redis;
    private final Cache<String, String> local;
    private final Duration ttl;
    private final AtomicLong redisDisabledUntil = new AtomicLong(0);

    public UrlCache(ObjectProvider<StringRedisTemplate> redis, AppProperties props) {
        this.redis = redis.getIfAvailable();
        this.ttl = Duration.ofHours(Math.max(1, props.getCache().getTtlHours()));
        this.local = Caffeine.newBuilder()
                .maximumSize(Math.max(100, props.getCache().getLocalMaxEntries()))
                .expireAfterWrite(ttl)
                .build();
    }

    public Optional<String> get(String code) {
        if (useRedis()) {
            try {
                return Optional.ofNullable(redis.opsForValue().get(KEY_PREFIX + code));
            } catch (RuntimeException e) {
                tripBreaker("get", e);
                return Optional.empty();
            }
        }
        return Optional.ofNullable(local.getIfPresent(code));
    }

    /** Stores the mapping now, or after the surrounding transaction commits. */
    public void put(String code, String url) {
        afterCommit(() -> doPut(code, url));
    }

    /** Removes the mapping now, or after the surrounding transaction commits. */
    public void evict(String code) {
        afterCommit(() -> doEvict(code));
    }

    private void doPut(String code, String url) {
        if (useRedis()) {
            try {
                redis.opsForValue().set(KEY_PREFIX + code, url, ttl);
            } catch (RuntimeException e) {
                tripBreaker("put", e);
            }
        } else {
            local.put(code, url);
        }
    }

    private void doEvict(String code) {
        // Evict from both stores: Redis may have been skipped while the breaker was open.
        local.invalidate(code);
        if (redis != null) {
            try {
                redis.delete(KEY_PREFIX + code);
            } catch (RuntimeException e) {
                tripBreaker("evict", e);
            }
        }
    }

    private boolean useRedis() {
        return redis != null && System.currentTimeMillis() >= redisDisabledUntil.get();
    }

    private void tripBreaker(String op, RuntimeException e) {
        redisDisabledUntil.set(System.currentTimeMillis() + BREAKER_OPEN.toMillis());
        log.warn("Redis cache {} failed; using in-memory cache for {}s: {}", op, BREAKER_OPEN.toSeconds(), e.getMessage());
    }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
