package com.example.shortener.service;

import com.example.shortener.config.AppProperties;
import com.example.shortener.entity.UrlAnalytics;
import com.example.shortener.repository.UrlAnalyticsRepository;
import com.example.shortener.repository.UrlRepository;
import com.example.shortener.security.ClientIpResolver;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

/**
 * Records clicks off the request thread, so a redirect never waits for the insert.
 * Skips bots and link-preview fetchers, and counts a repeat click from the same
 * visitor on the same link only once per de-duplication window.
 */
@Component
public class ClickRecorder {

    private static final Logger log = LoggerFactory.getLogger(ClickRecorder.class);
    private static final int MAX_FIELD = 512;

    private final UrlRepository urlRepository;
    private final UrlAnalyticsRepository analyticsRepository;
    private final Cache<String, Boolean> recentClicks;

    public ClickRecorder(UrlRepository urlRepository, UrlAnalyticsRepository analyticsRepository, AppProperties props) {
        this.urlRepository = urlRepository;
        this.analyticsRepository = analyticsRepository;
        this.recentClicks = Caffeine.newBuilder()
                .maximumSize(100_000)
                .expireAfterWrite(Duration.ofSeconds(Math.max(1, props.getAnalytics().getDedupeWindowSeconds())))
                .build();
    }

    /** True when the click should be stored (not a bot and not a repeat within the window). */
    boolean shouldCount(String shortCode, String ip, String userAgent) {
        if (UserAgents.isBot(userAgent)) {
            return false;
        }
        String key = shortCode + '|' + ip + '|' + userAgent.hashCode();
        return recentClicks.asMap().putIfAbsent(key, Boolean.TRUE) == null;
    }

    @Async("taskExecutor")
    public void record(String shortCode, Instant at, String ip, String userAgent, String referer) {
        try {
            if (!shouldCount(shortCode, ip, userAgent)) {
                return;
            }
            urlRepository.findByShortCode(shortCode).ifPresent(url -> analyticsRepository.save(new UrlAnalytics(
                    url, at, ClientIpResolver.anonymize(ip), truncate(userAgent), refererOrigin(referer))));
        } catch (RuntimeException e) {
            log.warn("Failed to record click for {}: {}", shortCode, e.getMessage());
        }
    }

    /** Keeps only scheme://host[:port]; paths and queries can contain personal data. */
    static String refererOrigin(String referer) {
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(referer.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return null;
            }
            String origin = scheme.toLowerCase(Locale.ROOT) + "://" + host.toLowerCase(Locale.ROOT)
                    + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            return truncate(origin);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= MAX_FIELD ? s : s.substring(0, MAX_FIELD);
    }
}
