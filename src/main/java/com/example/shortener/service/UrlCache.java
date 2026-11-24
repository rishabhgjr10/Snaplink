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
import java.time.Instant;
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
    /** v2 entries carry the link's expiry ("<epochMillis or 0>|<url>"); old "url:" entries are ignored. */
    private static final String KEY_PREFIX = "url2:";

    /** A cached destination and the moment it stops being valid (null = no expiry). */
    public record CachedLink(String url, Instant expiresAt) {}
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

    public Optional<CachedLink> get(String code) {
        String raw;
        if (useRedis()) {
            try {
                raw = redis.opsForValue().get(KEY_PREFIX + code);
            } catch (RuntimeException e) {
                tripBreaker("get", e);
                return Optional.empty();
            }
        } else {
            raw = local.getIfPresent(code);
        }
        return Optional.ofNullable(decode(raw));
    }

    /**
     * Stores the mapping now, or after the surrounding transaction commits. The entry's TTL is
     * capped at the time left until {@code expiresAt}, and readers re-check the expiry anyway.
     */
    public void put(String code, String url, Instant expiresAt) {
        afterCommit(() -> doPut(code, encode(url, expiresAt), ttlFor(expiresAt)));
    }

    private Duration ttlFor(Instant expiresAt) {
        if (expiresAt == null) {
            return ttl;
        }
        Duration left = Duration.between(Instant.now(), expiresAt);
        return left.compareTo(ttl) < 0 ? (left.isNegative() || left.isZero() ? Duration.ofSeconds(1) : left) : ttl;
    }

    static String encode(String url, Instant expiresAt) {
        return (expiresAt == null ? 0 : expiresAt.toEpochMilli()) + "|" + url;
    }

    static CachedLink decode(String raw) {
        if (raw == null) {
            return null;
        }
        int sep = raw.indexOf('|');
        if (sep <= 0) {
            return null;
        }
        try {
            long millis = Long.parseLong(raw.substring(0, sep));
            return new CachedLink(raw.substring(sep + 1), millis == 0 ? null : Instant.ofEpochMilli(millis));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Removes the mapping now, or after the surrounding transaction commits. */
    public void evict(String code) {
        afterCommit(() -> doEvict(code));
    }

    private void doPut(String code, String value, Duration entryTtl) {
        if (useRedis()) {
            try {
                redis.opsForValue().set(KEY_PREFIX + code, value, entryTtl);
            } catch (RuntimeException e) {
                tripBreaker("put", e);
            }
        } else {
            // The local cache has a fixed TTL; expiry is enforced by readers via CachedLink.expiresAt.
            local.put(code, value);
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
