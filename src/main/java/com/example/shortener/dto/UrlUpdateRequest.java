package com.example.shortener.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Body of PUT /api/v1/urls/{shortCode}. Omitted or blank fields are left unchanged.
 * A {@code clear*} flag removes that limit (for example clearExpiresAt makes the link permanent).
 */
public record UrlUpdateRequest(
        @Size(max = 2048, message = "URL must be at most 2048 characters.")
        String originalUrl,

        @Size(max = 32, message = "Alias must be at most 32 characters.")
        String customAlias,

        Instant expiresAt,
        Boolean clearExpiresAt,

        Instant startsAt,
        Boolean clearStartsAt,

        @Min(value = 1, message = "Click limit must be at least 1.")
        @Max(value = 1_000_000, message = "Click limit must be at most 1,000,000.")
        Integer maxClicks,
        Boolean clearMaxClicks) {

    public boolean changesLifetime() {
        return expiresAt != null || Boolean.TRUE.equals(clearExpiresAt)
                || startsAt != null || Boolean.TRUE.equals(clearStartsAt)
                || maxClicks != null || Boolean.TRUE.equals(clearMaxClicks);
    }
}
