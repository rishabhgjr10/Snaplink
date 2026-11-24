package com.example.shortener.dto;

import com.example.shortener.entity.LinkStatus;
import com.example.shortener.entity.UrlEntity;

import java.time.Instant;

/**
 * A link as shown to its owner.
 *
 * @param totalClicks lifetime human clicks
 * @param status      whether it redirects right now (ACTIVE, SCHEDULED, EXPIRED, LIMIT_REACHED, DISABLED)
 */
public record UrlItemDto(String shortCode, String originalUrl, Instant createdAt, long totalClicks, boolean disabled,
                         LinkStatus status, Instant expiresAt, Instant startsAt, Integer maxClicks) {

    public static UrlItemDto of(UrlEntity e, Instant now) {
        return new UrlItemDto(e.getShortCode(), e.getOriginalUrl(), e.getCreatedAt(), e.getClickCount(),
                e.isDisabled(), e.status(now), e.getExpiresAt(), e.getStartsAt(), e.getMaxClicks());
    }
}
