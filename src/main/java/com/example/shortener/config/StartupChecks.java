package com.example.shortener.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/** Logs configuration mistakes that would not stop the app but would weaken it in production. */
@Component
public class StartupChecks {

    private static final Logger log = LoggerFactory.getLogger(StartupChecks.class);

    private final AppProperties props;
    private final Environment env;

    public StartupChecks(AppProperties props, Environment env) {
        this.props = props;
        this.env = env;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void check() {
        if (!env.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }
        if (props.getBaseUrl() == null || props.getBaseUrl().contains("localhost")) {
            log.warn("APP_BASE_URL is not set to the public short-link origin; self-link detection relies on app.self-hosts only.");
        }
        if (props.getAdminUids().stream().allMatch(String::isBlank)) {
            log.warn("APP_ADMIN_UIDS is empty; nobody can review abuse reports or disable links.");
        }
        if (env.getProperty("spring.data.redis.host", "").isBlank()) {
            log.info("Redis not configured; using per-instance in-memory cache and rate limits.");
        }
    }
}
