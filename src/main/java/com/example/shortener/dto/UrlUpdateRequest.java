package com.example.shortener.dto;

import jakarta.validation.constraints.Size;

/** Body of PUT /api/v1/urls/{shortCode}. Omitted or blank fields are left unchanged. */
public record UrlUpdateRequest(
        @Size(max = 2048, message = "URL must be at most 2048 characters.")
        String originalUrl,

        @Size(max = 32, message = "Alias must be at most 32 characters.")
        String customAlias) {
}
