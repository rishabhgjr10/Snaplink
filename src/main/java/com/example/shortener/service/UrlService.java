package com.example.shortener.service;

import com.example.shortener.dto.UrlShortenRequest;
import com.example.shortener.dto.UrlShortenResponse;
import com.example.shortener.dto.UrlAnalyticsResponse;
import com.example.shortener.dto.UrlItemDto;
import com.example.shortener.dto.UrlUpdateRequest;
import java.util.List;
import java.util.Optional;

public interface UrlService {
    UrlShortenResponse shortenUrl(UrlShortenRequest request, String baseUrl);
    UrlAnalyticsResponse getAnalytics(String shortCode);
    Optional<String> resolveOriginalUrl(String shortCode);
    void recordClick(String shortCode, String ipAddress, String userAgent, String referer);
    List<UrlItemDto> getAllUrls(String baseUrl);
    UrlItemDto updateUrl(String shortCode, UrlUpdateRequest request, String baseUrl);
    void deleteUrl(String shortCode);
}
