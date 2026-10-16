package com.example.shortener.controller;

import com.example.shortener.dto.UrlShortenRequest;
import com.example.shortener.dto.UrlShortenResponse;
import com.example.shortener.dto.UrlAnalyticsResponse;
import com.example.shortener.dto.UrlItemDto;
import com.example.shortener.dto.UrlUpdateRequest;
import com.example.shortener.service.UrlService;
import com.example.shortener.util.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {

    private final UrlService urlService;

    @Value("${app.base-url:http://localhost:8080}")
    private String configuredBaseUrl;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    private String resolveBaseUrl(String host) {
        if (configuredBaseUrl != null && !configuredBaseUrl.isBlank() && !configuredBaseUrl.contains("localhost")) {
            return configuredBaseUrl.replaceAll("/+$", "");
        }
        if (host != null && !host.isBlank() && !host.contains("localhost")) {
            String scheme = host.contains("localhost") ? "http" : "https";
            return (scheme + "://" + host).replaceAll("/+$", "");
        }
        return (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) 
                ? configuredBaseUrl.replaceAll("/+$", "") 
                : ("http://" + host);
    }

    @PostMapping("/shorten")
    public ResponseEntity<ApiResponse<UrlShortenResponse>> shorten(@Validated @RequestBody UrlShortenRequest request,
                                                                 @RequestHeader(value = "Host", defaultValue = "localhost:8080") String host) {
        String baseUrl = resolveBaseUrl(host);
        UrlShortenResponse resp = urlService.shortenUrl(request, baseUrl);
        return ResponseEntity.ok(ApiResponse.success(resp, "URL shortened", 200));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UrlItemDto>>> getAllUrls(@RequestHeader(value = "Host", defaultValue = "localhost:8080") String host) {
        String baseUrl = resolveBaseUrl(host);
        List<UrlItemDto> list = urlService.getAllUrls(baseUrl);
        return ResponseEntity.ok(ApiResponse.success(list, "URLs retrieved", 200));
    }

    @PutMapping("/{shortCode}")
    public ResponseEntity<ApiResponse<UrlItemDto>> updateUrl(@PathVariable(value = "shortCode") String shortCode,
                                                             @RequestBody UrlUpdateRequest request,
                                                             @RequestHeader(value = "Host", defaultValue = "localhost:8080") String host) {
        String baseUrl = resolveBaseUrl(host);
        UrlItemDto updated = urlService.updateUrl(shortCode, request, baseUrl);
        return ResponseEntity.ok(ApiResponse.success(updated, "URL updated successfully", 200));
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<ApiResponse<Void>> deleteUrl(@PathVariable(value = "shortCode") String shortCode) {
        urlService.deleteUrl(shortCode);
        return ResponseEntity.ok(ApiResponse.success(null, "URL deleted successfully", 200));
    }

    @GetMapping("/{shortCode}/analytics")
    public ResponseEntity<ApiResponse<UrlAnalyticsResponse>> analytics(@PathVariable(value = "shortCode") String shortCode) {
        var data = urlService.getAnalytics(shortCode);
        UrlAnalyticsResponse resp = new UrlAnalyticsResponse(data.getTotalClicks(), data.getRecentVisits());
        return ResponseEntity.ok(ApiResponse.success(resp, "Analytics fetched", 200));
    }

    @GetMapping("/{shortCode}/resolve")
    public ResponseEntity<ApiResponse<Map<String, String>>> resolve(@PathVariable(value = "shortCode") String shortCode) {
        var originalOpt = urlService.resolveOriginalUrl(shortCode);
        if (originalOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("shortCode", shortCode, "originalUrl", originalOpt.get()),
                "Resolved", 200));
    }
}
