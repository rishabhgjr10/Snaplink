package com.example.shortener.service;

import com.example.shortener.config.AppProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

/**
 * Token-bucket rate limiting, distributed through Redis when configured.
 * Without Redis (or if it is unreachable) a bounded in-memory bucket store is used,
 * so limits keep working on a single instance instead of failing open.
 */
@Service
public class RateLimitingService {

    /** Independent budgets. */
    public enum Plan { READ, WRITE, ANONYMOUS_GLOBAL }

    private static final Logger log = LoggerFactory.getLogger(RateLimitingService.class);
    private static final String KEY_PREFIX = "rate_limit:";

    private final LettuceConnectionFactory connectionFactory;
    private final Map<Plan, Integer> perMinute = new EnumMap<>(Plan.class);
    private final Cache<String, Bucket> localBuckets = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    private volatile ProxyManager<byte[]> proxyManager;
    private volatile boolean redisInitAttempted;

    public RateLimitingService(ObjectProvider<LettuceConnectionFactory> connectionFactory, AppProperties props) {
        this.connectionFactory = connectionFactory.getIfAvailable();
        AppProperties.RateLimit limits = props.getRateLimit();
        perMinute.put(Plan.READ, limits.getReadPerMinute());
        perMinute.put(Plan.WRITE, limits.getWritePerMinute());
        perMinute.put(Plan.ANONYMOUS_GLOBAL, limits.getAnonymousGlobalPerMinute());
    }

    /** Consumes one token from the {@code plan} bucket identified by {@code key}. */
    public boolean tryConsume(Plan plan, String key) {
        ProxyManager<byte[]> pm = getProxyManager();
        if (pm != null) {
            try {
                byte[] redisKey = (KEY_PREFIX + plan.name().toLowerCase() + ':' + key).getBytes(StandardCharsets.UTF_8);
                return pm.builder().build(redisKey, () -> config(plan)).tryConsume(1);
            } catch (RuntimeException e) {
                log.warn("Redis rate limiter unavailable, using in-memory buckets: {}", e.getMessage());
            }
        }
        return localBuckets.get(plan.name() + ':' + key, k -> Bucket.builder().addLimit(bandwidth(plan)).build())
                .tryConsume(1);
    }

    private BucketConfiguration config(Plan plan) {
        return BucketConfiguration.builder().addLimit(bandwidth(plan)).build();
    }

    private Bandwidth bandwidth(Plan plan) {
        int n = Math.max(1, perMinute.get(plan));
        return Bandwidth.classic(n, Refill.greedy(n, Duration.ofMinutes(1)));
    }

    private ProxyManager<byte[]> getProxyManager() {
        if (!redisInitAttempted) {
            synchronized (this) {
                if (!redisInitAttempted) {
                    proxyManager = initProxyManager();
                    redisInitAttempted = true;
                }
            }
        }
        return proxyManager;
    }

    private ProxyManager<byte[]> initProxyManager() {
        if (connectionFactory == null) {
            log.info("Redis not configured; using in-memory rate limiting.");
            return null;
        }
        try {
            AbstractRedisClient client = connectionFactory.getNativeClient();
            ExpirationAfterWriteStrategy expiration =
                    ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(1));
            if (client instanceof RedisClient redisClient) {
                return LettuceBasedProxyManager.builderFor(redisClient).withExpirationStrategy(expiration).build();
            }
            if (client instanceof RedisClusterClient clusterClient) {
                return LettuceBasedProxyManager.builderFor(clusterClient).withExpirationStrategy(expiration).build();
            }
            log.warn("Unsupported Redis client {}; using in-memory rate limiting.", client);
        } catch (RuntimeException e) {
            log.warn("Could not initialise Redis rate limiter; using in-memory rate limiting: {}", e.getMessage());
        }
        return null;
    }
}
