package com.example.shortener.config;

import com.example.shortener.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * HandlerInterceptor that throttles POST requests to the URL shorten endpoint.
 * Returns HTTP 429 (Too Many Requests) when the client IP exceeds the configured rate.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private final RateLimitingService rateLimitingService;

    public RateLimitInterceptor(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Only rate-limit POST requests
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String clientIp = rateLimitingService.getClientIp(request);
        boolean allowed = rateLimitingService.tryConsume(clientIp);

        if (!allowed) {
            log.warn("Rate limit exceeded for client IP: {} on endpoint: {}", clientIp, request.getRequestURI());
            sendTooManyRequestsResponse(response);
            return false;
        }

        return true;
    }

    private void sendTooManyRequestsResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"Rate limit exceeded. Please try again later.\",\"status\":429}");
        response.getWriter().flush();
    }
}
