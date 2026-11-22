package com.example.shortener.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

/**
 * Redis connection, created only when {@code spring.data.redis.host} is set.
 * Without it the app uses bounded in-memory caches and rate-limit buckets,
 * instead of trying (and timing out on) a Redis that does not exist.
 */
@Configuration
@ConditionalOnExpression("!'${spring.data.redis.host:}'.trim().isEmpty()")
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean redisSsl;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost.trim(), redisPort);
        if (redisPassword != null && !redisPassword.isBlank()) {
            redisConfig.setPassword(RedisPassword.of(redisPassword));
        }
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2));
        if (redisSsl) {
            clientConfig.useSsl();
        }
        return new LettuceConnectionFactory(redisConfig, clientConfig.build());
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
