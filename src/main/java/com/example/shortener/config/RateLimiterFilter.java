package com.example.shortener.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter that applies a Bucket4j token bucket per client IP.
 * It is scoped only to the POST /api/v1/urls/shorten endpoint as required.
 * Other endpoints (including GET /{shortCode}) are not rate limited.
 */
@Component
public class RateLimiterFilter extends OncePerRequestFilter {

    private static final int REQUESTS_PER_MINUTE = 20;
    // Cache of buckets per IP address
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    private Bucket resolveBucket(String ip) {
        return bucketCache.computeIfAbsent(ip, k -> {
            Refill refill = Refill.greedy(REQUESTS_PER_MINUTE, Duration.ofMinutes(1));
            Bandwidth limit = Bandwidth.classic(REQUESTS_PER_MINUTE, refill);
            return Bucket.builder().addLimit(limit).build();
        });
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        // Apply rate limiting only to the shorten endpoint
        if ("POST".equalsIgnoreCase(request.getMethod()) && path.startsWith("/api/v1/urls/shorten")) {
            String ip = request.getRemoteAddr();
            Bucket bucket = resolveBucket(ip);
            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded\"}");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
