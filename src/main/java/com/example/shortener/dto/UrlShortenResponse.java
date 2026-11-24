package com.example.shortener.dto;

import java.time.Instant;

/** The public short-link URL is built by the client from its configured short-link origin. */
public record UrlShortenResponse(String shortCode, String originalUrl, Instant expiresAt, Instant startsAt,
                                 Integer maxClicks) {
}
