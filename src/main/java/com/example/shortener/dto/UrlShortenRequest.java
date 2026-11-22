package com.example.shortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of POST /api/v1/urls/shorten. URL and alias rules are enforced by UrlPolicy. */
public record UrlShortenRequest(
        @NotBlank(message = "Enter a URL.")
        @Size(max = 2048, message = "URL must be at most 2048 characters.")
        String originalUrl,

        @Size(max = 32, message = "Alias must be at most 32 characters.")
        String customAlias) {
}
