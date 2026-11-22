package com.example.shortener.service;

import com.example.shortener.config.AppProperties;
import com.example.shortener.repository.UrlAnalyticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/** Deletes click records older than {@code app.analytics.retention-days}, once a day. */
@Component
public class AnalyticsRetentionJob {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsRetentionJob.class);

    private final UrlAnalyticsRepository analyticsRepository;
    private final int retentionDays;

    public AnalyticsRetentionJob(UrlAnalyticsRepository analyticsRepository, AppProperties props) {
        this.analyticsRepository = analyticsRepository;
        this.retentionDays = props.getAnalytics().getRetentionDays();
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    @Transactional
    public void purge() {
        if (retentionDays <= 0) {
            return; // retention disabled
        }
        int deleted = analyticsRepository.deleteOlderThan(Instant.now().minus(Duration.ofDays(retentionDays)));
        if (deleted > 0) {
            log.info("Purged {} click records older than {} days", deleted, retentionDays);
        }
    }
}
