package com.example.shortener.config;

import com.example.shortener.security.ClientIpResolver;
import com.example.shortener.security.CurrentUser;
import com.example.shortener.service.RateLimitingService;
import com.example.shortener.service.RateLimitingService.Plan;
import com.example.shortener.util.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

/**
 * Rate limits every /api request (see WebConfig for exclusions).
 * Signed-in callers are keyed by Firebase UID, which cannot be forged; anonymous
 * callers by client IP, plus a shared budget for all anonymous writes so that
 * forged proxy headers cannot multiply an attacker's allowance without bound.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    static final String MESSAGE = "Rate limit exceeded. Please try again later.";

    private final RateLimitingService rateLimitingService;
    private final CurrentUser currentUser;
    private final ClientIpResolver ipResolver;
    private final ObjectMapper objectMapper;

    public RateLimitInterceptor(RateLimitingService rateLimitingService, CurrentUser currentUser,
                                ClientIpResolver ipResolver, ObjectMapper objectMapper) {
        this.rateLimitingService = rateLimitingService;
        this.currentUser = currentUser;
        this.ipResolver = ipResolver;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String method = request.getMethod();
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        boolean read = "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method);
        Plan plan = read ? Plan.READ : Plan.WRITE;

        Optional<String> uid = currentUser.uid();
        String key = uid.map(u -> "user:" + u).orElseGet(() -> "ip:" + ipResolver.resolve(request));

        boolean allowed = rateLimitingService.tryConsume(plan, key);
        if (allowed && uid.isEmpty() && !read) {
            allowed = rateLimitingService.tryConsume(Plan.ANONYMOUS_GLOBAL, "all");
        }
        if (!allowed) {
            log.warn("Rate limit exceeded: key={} plan={} uri={}", key, plan, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ApiResponse.error(MESSAGE, 429));
            return false;
        }
        return true;
    }
}
