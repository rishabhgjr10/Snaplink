package com.example.shortener.dto;

import java.time.Instant;

public record UrlItemDto(String shortCode, String originalUrl, Instant createdAt, long totalClicks, boolean disabled) {
}
