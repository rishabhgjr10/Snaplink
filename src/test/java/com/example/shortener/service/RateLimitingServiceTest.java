package com.example.shortener.service;

import com.example.shortener.config.AppProperties;
import com.example.shortener.service.RateLimitingService.Plan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RateLimitingServiceTest {

    private RateLimitingService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        AppProperties props = new AppProperties();
        props.getRateLimit().setWritePerMinute(3);
        props.getRateLimit().setReadPerMinute(5);
        // No Redis: the bounded in-memory bucket store is used.
        service = new RateLimitingService(mock(ObjectProvider.class), props);
    }

    @Test
    void enforcesLimitPerKey() {
        for (int i = 0; i < 3; i++) {
            assertThat(service.tryConsume(Plan.WRITE, "ip:1.1.1.1")).isTrue();
        }
        assertThat(service.tryConsume(Plan.WRITE, "ip:1.1.1.1")).isFalse();
        assertThat(service.tryConsume(Plan.WRITE, "ip:2.2.2.2")).isTrue();
    }

    @Test
    void plansHaveIndependentBudgets() {
        for (int i = 0; i < 3; i++) {
            service.tryConsume(Plan.WRITE, "user:a");
        }
        assertThat(service.tryConsume(Plan.WRITE, "user:a")).isFalse();
        for (int i = 0; i < 5; i++) {
            assertThat(service.tryConsume(Plan.READ, "user:a")).isTrue();
        }
        assertThat(service.tryConsume(Plan.READ, "user:a")).isFalse();
    }

    @Test
    void worksWithoutRedisBean() {
        @SuppressWarnings("unchecked")
        ObjectProvider<LettuceConnectionFactory> none = mock(ObjectProvider.class);
        RateLimitingService local = new RateLimitingService(none, new AppProperties());
        assertThat(local.tryConsume(Plan.READ, "k")).isTrue();
    }
}
