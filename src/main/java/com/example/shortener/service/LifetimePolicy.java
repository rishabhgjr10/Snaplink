package com.example.shortener.service;

import com.example.shortener.config.AppProperties;
import com.example.shortener.exception.ApiException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Rules for when a link starts and stops working.
 * <ul>
 *   <li>Expiry is at least {@code app.links.min-lifetime-minutes} and at most
 *       {@code app.links.max-lifetime-days} away.</li>
 *   <li>A start time is at most {@code max-lifetime-days} away and before the expiry.</li>
 *   <li>Links created without signing in always expire, after at most
 *       {@code app.links.anonymous-max-days} (which is also their default).</li>
 * </ul>
 */
@Component
public class LifetimePolicy {

    /** Normalised lifetime settings for a link. */
    public record Lifetime(Instant expiresAt, Instant startsAt, Integer maxClicks) {}

    private final Duration minLifetime;
    private final Duration maxLifetime;
    private final Duration anonymousMax;

    public LifetimePolicy(AppProperties props) {
        AppProperties.Links links = props.getLinks();
        this.minLifetime = Duration.ofMinutes(Math.max(1, links.getMinLifetimeMinutes()));
        this.maxLifetime = Duration.ofDays(Math.max(1, links.getMaxLifetimeDays()));
        this.anonymousMax = Duration.ofDays(Math.max(1, links.getAnonymousMaxDays()));
    }

    /** Lifetime for a new link. */
    public Lifetime forNewLink(Instant expiresAt, Instant startsAt, Integer maxClicks, boolean anonymous, Instant now) {
        Instant expiry = expiresAt;
        if (anonymous) {
            if (expiry == null) {
                expiry = now.plus(anonymousMax);
            } else if (expiry.isAfter(now.plus(anonymousMax).plusSeconds(60))) {
                throw new ApiException.InvalidInput("Links created without signing in can last at most "
                        + anonymousMax.toDays() + " days. Sign in for longer-lasting links.");
            }
        }
        return check(expiry, true, startsAt, true, maxClicks, now);
    }

    /**
     * Lifetime after an edit. Only values that changed are checked against "now", so editing
     * something else on an expired link does not fail because its old expiry is in the past.
     */
    public Lifetime forUpdate(Instant expiresAt, boolean expiryChanged, Instant startsAt, boolean startChanged,
                              Integer maxClicks, Instant now) {
        return check(expiresAt, expiryChanged, startsAt, startChanged, maxClicks, now);
    }

    private Lifetime check(Instant expiresAt, boolean checkExpiry, Instant startsAt, boolean checkStart,
                           Integer maxClicks, Instant now) {
        if (expiresAt != null && checkExpiry) {
            if (expiresAt.isBefore(now.plus(minLifetime))) {
                throw new ApiException.InvalidInput(
                        "Expiry must be at least " + minLifetime.toMinutes() + " minutes from now.");
            }
            if (expiresAt.isAfter(now.plus(maxLifetime))) {
                throw new ApiException.InvalidInput(
                        "Expiry can be at most " + maxLifetime.toDays() + " days from now.");
            }
        }
        if (startsAt != null && checkStart && startsAt.isAfter(now.plus(maxLifetime))) {
            throw new ApiException.InvalidInput(
                    "Start time can be at most " + maxLifetime.toDays() + " days from now.");
        }
        if (startsAt != null && expiresAt != null && !startsAt.isBefore(expiresAt)) {
            throw new ApiException.InvalidInput("Start time must be before the expiry time.");
        }
        if (maxClicks != null && (maxClicks < 1 || maxClicks > 1_000_000)) {
            throw new ApiException.InvalidInput("Click limit must be between 1 and 1,000,000.");
        }
        return new Lifetime(expiresAt, startsAt, maxClicks);
    }
}
