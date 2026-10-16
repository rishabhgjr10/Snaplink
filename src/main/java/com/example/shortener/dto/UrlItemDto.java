package com.example.shortener.dto;

import java.time.Instant;

public class UrlItemDto {
    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private Instant createdAt;
    private long totalClicks;

    public UrlItemDto() {}

    public UrlItemDto(String shortCode, String shortUrl, String originalUrl, Instant createdAt, long totalClicks) {
        this.shortCode = shortCode;
        this.shortUrl = shortUrl;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.totalClicks = totalClicks;
    }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getShortUrl() { return shortUrl; }
    public void setShortUrl(String shortUrl) { this.shortUrl = shortUrl; }
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public long getTotalClicks() { return totalClicks; }
    public void setTotalClicks(long totalClicks) { this.totalClicks = totalClicks; }
}
