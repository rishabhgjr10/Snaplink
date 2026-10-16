package com.example.shortener.dto;

public class UrlUpdateRequest {
    private String originalUrl;
    private String customAlias;

    public UrlUpdateRequest() {}

    public UrlUpdateRequest(String originalUrl, String customAlias) {
        this.originalUrl = originalUrl;
        this.customAlias = customAlias;
    }

    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public String getCustomAlias() { return customAlias; }
    public void setCustomAlias(String customAlias) { this.customAlias = customAlias; }
}
