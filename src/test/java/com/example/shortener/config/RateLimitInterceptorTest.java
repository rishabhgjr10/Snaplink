package com.example.shortener.config;

import com.example.shortener.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitInterceptorTest {

    private RateLimitingService rateLimitingService;
    private RateLimitInterceptor interceptor;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        rateLimitingService = new RateLimitingService(null) {
            @Override
            public boolean tryConsume(String clientIp) {
                return !"blocked-ip".equals(clientIp);
            }

            @Override
            public String getClientIp(HttpServletRequest req) {
                String ip = req.getHeader("X-Forwarded-For");
                return ip != null ? ip : "127.0.0.1";
            }
        };
        interceptor = new RateLimitInterceptor(rateLimitingService);

        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);

        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    void testNonPostRequestsAreBypassed() throws Exception {
        when(request.getMethod()).thenReturn("GET");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(response, never()).setStatus(429);
    }

    @Test
    void testAllowedPostRequest() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Forwarded-For")).thenReturn("allowed-ip");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
        verify(response, never()).setStatus(429);
    }

    @Test
    void testBlockedPostRequestReturns429() throws Exception {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Forwarded-For")).thenReturn("blocked-ip");
        when(request.getRequestURI()).thenReturn("/api/v1/urls/shorten");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        verify(response).setStatus(429);
        verify(response).setContentType("application/json");

        String body = responseWriter.toString();
        assertTrue(body.contains("Rate limit exceeded. Please try again later."));
        assertTrue(body.contains("429"));
    }
}
