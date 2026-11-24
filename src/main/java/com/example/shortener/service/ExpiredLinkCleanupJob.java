package com.example.shortener.service;

import com.example.shortener.config.AppProperties;
import com.example.shortener.entity.UrlEntity;
import com.example.shortener.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * Deletes links created without signing in once they have been expired for
 * {@code app.links.anonymous-expired-retention-days}, which frees their alias.
 * Links that belong to an account are never deleted automatically: the owner
 * can still see their analytics and reactivate them.
 */
@Component
public class ExpiredLinkCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiredLinkCleanupJob.class);
    private static final int BATCH = 500;

    private final UrlRepository urlRepository;
    private final UrlCache cache;
    private final Clock clock;
    private final int retentionDays;

    public ExpiredLinkCleanupJob(UrlRepository urlRepository, UrlCache cache, Clock clock, AppProperties props) {
        this.urlRepository = urlRepository;
        this.cache = cache;
        this.clock = clock;
        this.retentionDays = props.getLinks().getAnonymousExpiredRetentionDays();
    }

    @Scheduled(cron = "0 45 3 * * *", zone = "UTC")
    @Transactional
    public int purge() {
        if (retentionDays <= 0) {
            return 0; // cleanup disabled
        }
        var cutoff = clock.instant().minus(Duration.ofDays(retentionDays));
        int total = 0;
        List<UrlEntity> batch;
        do {
            batch = urlRepository.findAnonymousExpiredBefore(cutoff, PageRequest.of(0, BATCH));
            if (!batch.isEmpty()) {
                urlRepository.deleteByIdsBulk(batch.stream().map(UrlEntity::getId).toList());
                batch.forEach(u -> cache.evict(u.getShortCode()));
                total += batch.size();
            }
        } while (batch.size() == BATCH);
        if (total > 0) {
            log.info("Deleted {} anonymous links expired more than {} days ago", total, retentionDays);
        }
        return total;
    }
}
