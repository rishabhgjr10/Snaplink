package com.example.shortener.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.cluster.RedisClusterClient;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Distributed rate-limiting service backed by Bucket4j and Redis.
 * Falls back to an in-memory token bucket if Redis is not configured,
 * and fails open if Redis is temporarily unreachable.
 */
@Service
public class RateLimitingService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);
    private static final int REQUESTS_PER_MINUTE = 20;
    private static final String KEY_PREFIX = "rate_limit:url_shorten:";

    private final RedisConnectionFactory connectionFactory;
    private volatile ProxyManager<byte[]> proxyManager;
    private volatile boolean redisInitAttempted = false;
    private final Map<String, Bucket> fallbackBucketCache = new ConcurrentHashMap<>();

    public RateLimitingService(@Autowired(required = false) RedisConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    private ProxyManager<byte[]> getProxyManager() {
        if (!redisInitAttempted) {
            synchronized (this) {
                if (!redisInitAttempted) {
                    this.proxyManager = initProxyManager();
                    this.redisInitAttempted = true;
                }
            }
        }
        return this.proxyManager;
    }

    private ProxyManager<byte[]> initProxyManager() {
        if (!(connectionFactory instanceof LettuceConnectionFactory lettuceFactory)) {
            log.info("RedisConnectionFactory is not LettuceConnectionFactory (or null). Using in-memory rate-limiting fallback.");
            return null;
        }

        try {
            AbstractRedisClient nativeClient = lettuceFactory.getNativeClient();
            if (nativeClient == null) {
                log.info("Lettuce native client not yet initialized on factory. Using in-memory fallback.");
                return null;
            }

            ExpirationAfterWriteStrategy expiration =
                    ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(1));

            if (nativeClient instanceof RedisClient redisClient) {
                log.info("Initialized distributed Bucket4j rate limiter with Lettuce RedisClient.");
                return LettuceBasedProxyManager.builderFor(redisClient)
                        .withExpirationStrategy(expiration)
                        .build();
            } else if (nativeClient instanceof RedisClusterClient clusterClient) {
                log.info("Initialized distributed Bucket4j rate limiter with Lettuce RedisClusterClient.");
                return LettuceBasedProxyManager.builderFor(clusterClient)
                        .withExpirationStrategy(expiration)
                        .build();
            } else {
                log.warn("Unrecognized Lettuce native client type: {}. Using in-memory fallback.", nativeClient.getClass().getName());
                return null;
            }
        } catch (Exception e) {
            log.warn("Failed to initialize LettuceBasedProxyManager for Redis rate limiter: {}. Using in-memory fallback.", e.getMessage());
            return null;
        }
    }

    /**
     * Attempts to consume 1 token for the specified client IP.
     * Fails open (returns true) if Redis throws an exception.
     *
     * @param clientIp Client IP address
     * @return true if allowed, false if rate limit exceeded
     */
    public boolean tryConsume(String clientIp) {
        try {
            ProxyManager<byte[]> pm = getProxyManager();
            if (pm != null) {
                byte[] key = (KEY_PREFIX + clientIp).getBytes(StandardCharsets.UTF_8);
                Bucket bucket = pm.builder().build(key, this::createBucketConfig);
                return bucket.tryConsume(1);
            } else {
                Bucket bucket = fallbackBucketCache.computeIfAbsent(clientIp, k ->
                        Bucket.builder().addLimit(createBandwidth()).build());
                return bucket.tryConsume(1);
            }
        } catch (Exception e) {
            log.warn("Redis rate-limiter unavailable, failing open: {}", e.getMessage());
            return true; // Fail open to protect core shortening availability
        }
    }

    private BucketConfiguration createBucketConfig() {
        return BucketConfiguration.builder()
                .addLimit(createBandwidth())
                .build();
    }

    private Bandwidth createBandwidth() {
        Refill refill = Refill.greedy(REQUESTS_PER_MINUTE, Duration.ofMinutes(1));
        return Bandwidth.classic(REQUESTS_PER_MINUTE, refill);
    }

    /**
     * Extracts the client IP from proxy headers (e.g. X-Forwarded-For) or the remote address.
     */
    public String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String firstIp = xForwardedFor.split(",")[0].trim();
            if (!firstIp.isEmpty()) {
                return firstIp;
            }
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }
}
