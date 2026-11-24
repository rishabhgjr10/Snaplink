package com.example.shortener.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Body of POST /api/v1/urls/shorten. URL, alias and lifetime rules are enforced by UrlPolicy.
 *
 * @param expiresAt when the link stops working (ISO-8601, e.g. 2026-12-01T10:00:00Z); null = never
 *                  (signed-out users always get an expiry, see app.links.anonymous-max-days)
 * @param startsAt  when the link starts working; null = immediately
 * @param maxClicks stop after this many clicks; null = unlimited
 */
public record UrlShortenRequest(
        @NotBlank(message = "Enter a URL.")
        @Size(max = 2048, message = "URL must be at most 2048 characters.")
        String originalUrl,

        @Size(max = 32, message = "Alias must be at most 32 characters.")
        String customAlias,

        Instant expiresAt,

        Instant startsAt,

        @Min(value = 1, message = "Click limit must be at least 1.")
        @Max(value = 1_000_000, message = "Click limit must be at most 1,000,000.")
        Integer maxClicks) {

    /** Convenience for callers that only set a destination and alias. */
    public UrlShortenRequest(String originalUrl, String customAlias) {
        this(originalUrl, customAlias, null, null, null);
    }
}
