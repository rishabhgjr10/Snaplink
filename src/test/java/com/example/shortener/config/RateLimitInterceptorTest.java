package com.example.shortener.config;

import com.example.shortener.security.ClientIpResolver;
import com.example.shortener.security.CurrentUser;
import com.example.shortener.service.RateLimitingService;
import com.example.shortener.service.RateLimitingService.Plan;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitInterceptorTest {

    private RateLimitingService limiter;
    private CurrentUser currentUser;
    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        limiter = mock(RateLimitingService.class);
        currentUser = mock(CurrentUser.class);
        AppProperties props = new AppProperties();
        props.getRateLimit().setTrustedProxyHops(0);
        interceptor = new RateLimitInterceptor(limiter, currentUser, new ClientIpResolver(props), new ObjectMapper());
    }

    private static MockHttpServletRequest request(String method) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, "/api/v1/urls");
        req.setRemoteAddr("198.51.100.1");
        return req;
    }

    @Test
    void signedInCallersAreKeyedByUid() throws Exception {
        when(currentUser.uid()).thenReturn(Optional.of("uid-1"));
        when(limiter.tryConsume(any(), anyString())).thenReturn(true);

        assertThat(interceptor.preHandle(request("DELETE"), new MockHttpServletResponse(), new Object())).isTrue();
        verify(limiter).tryConsume(Plan.WRITE, "user:uid-1");
        verify(limiter, never()).tryConsume(eq(Plan.ANONYMOUS_GLOBAL), anyString());
    }

    @Test
    void anonymousWritesAlsoUseTheGlobalBudget() throws Exception {
        when(currentUser.uid()).thenReturn(Optional.empty());
        when(limiter.tryConsume(any(), anyString())).thenReturn(true);

        assertThat(interceptor.preHandle(request("POST"), new MockHttpServletResponse(), new Object())).isTrue();
        verify(limiter).tryConsume(Plan.WRITE, "ip:198.51.100.1");
        verify(limiter).tryConsume(Plan.ANONYMOUS_GLOBAL, "all");
    }

    @Test
    void readsUseTheReadBudget() throws Exception {
        when(currentUser.uid()).thenReturn(Optional.empty());
        when(limiter.tryConsume(any(), anyString())).thenReturn(true);

        interceptor.preHandle(request("GET"), new MockHttpServletResponse(), new Object());
        verify(limiter).tryConsume(Plan.READ, "ip:198.51.100.1");
        verify(limiter, never()).tryConsume(eq(Plan.ANONYMOUS_GLOBAL), anyString());
    }

    @Test
    void blockedRequestsGetJson429() throws Exception {
        when(currentUser.uid()).thenReturn(Optional.empty());
        when(limiter.tryConsume(any(), anyString())).thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request("POST"), response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString()).contains(RateLimitInterceptor.MESSAGE).contains("429");
    }
}
