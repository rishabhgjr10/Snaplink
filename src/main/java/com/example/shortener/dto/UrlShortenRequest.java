package com.example.shortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UrlShortenRequest {
    private String originalUrl;

    // Optional custom alias (short code). If provided, must be alphanumeric and 6-10 chars.
    private String customAlias;

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getCustomAlias() {
        return customAlias;
    }
}
