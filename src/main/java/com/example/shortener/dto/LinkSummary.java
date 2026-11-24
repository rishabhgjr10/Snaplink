package com.example.shortener.dto;

/**
 * Totals across all of the caller's links (not just the loaded page).
 *
 * @param expiredLinks links past their expiry time or click limit
 */
public record LinkSummary(long totalLinks, long totalClicks, long activeLinks, long expiredLinks) {
}
