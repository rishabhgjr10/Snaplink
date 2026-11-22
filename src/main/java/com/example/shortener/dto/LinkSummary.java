package com.example.shortener.dto;

/** Totals across all of the caller's links (not just the loaded page). */
public record LinkSummary(long totalLinks, long totalClicks) {
}
