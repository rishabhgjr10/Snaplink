package com.example.shortener.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        // null connectionFactory activates the in-memory fallback
        rateLimitingService = new RateLimitingService(null);
    }

    @Test
    void testConsumeWithinLimit() {
        String ip = "192.168.1.10";
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitingService.tryConsume(ip), "Request " + (i + 1) + " should be allowed");
        }
    }

    @Test
    void testConsumeExceedsLimit() {
        String ip = "192.168.1.20";
        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitingService.tryConsume(ip));
        }
        // 21st request exceeds 20 req/min
        assertFalse(rateLimitingService.tryConsume(ip), "21st request must be rate limited");
    }

    @Test
    void testIndependentBucketsPerIp() {
        String ipA = "10.0.0.1";
        String ipB = "10.0.0.2";

        for (int i = 0; i < 20; i++) {
            assertTrue(rateLimitingService.tryConsume(ipA));
        }
        assertFalse(rateLimitingService.tryConsume(ipA));

        // ipB should still have full quota
        assertTrue(rateLimitingService.tryConsume(ipB));
    }

    @Test
    void testClientIpExtractionFromXForwardedFor() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 70.41.3.18, 150.172.238.178");

        String ip = rateLimitingService.getClientIp(request);
        assertEquals("203.0.113.195", ip);
    }

    @Test
    void testClientIpExtractionFromXRealIp() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("198.51.100.1");

        String ip = rateLimitingService.getClientIp(request);
        assertEquals("198.51.100.1", ip);
    }

    @Test
    void testClientIpExtractionFallbackToRemoteAddr() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        String ip = rateLimitingService.getClientIp(request);
        assertEquals("127.0.0.1", ip);
    }
}
