package com.example.shortener.service;

import com.example.shortener.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Google Safe Browsing v4 lookup. Disabled when no API key is configured.
 * Fails open: if Google is unreachable, the link is allowed and a warning is logged.
 */
@Component
public class SafeBrowsingClient {

    private static final Logger log = LoggerFactory.getLogger(SafeBrowsingClient.class);
    private static final String ENDPOINT = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=";

    private final String apiKey;
    private final ObjectMapper objectMapper;
    /** Created only when a key is configured. */
    private final HttpClient http;

    public SafeBrowsingClient(AppProperties props, ObjectMapper objectMapper) {
        this.apiKey = props.getSafeBrowsingApiKey() == null ? "" : props.getSafeBrowsingApiKey().trim();
        this.objectMapper = objectMapper;
        this.http = apiKey.isEmpty() ? null
                : HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    public boolean isEnabled() {
        return !apiKey.isEmpty();
    }

    /** True when Google lists the URL as malware, phishing or unwanted software. */
    public boolean isUnsafe(String url) {
        if (!isEnabled()) {
            return false;
        }
        try {
            Map<String, Object> body = Map.of(
                    "client", Map.of("clientId", "snaplink", "clientVersion", "1.0"),
                    "threatInfo", Map.of(
                            "threatTypes", List.of("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE",
                                    "POTENTIALLY_HARMFUL_APPLICATION"),
                            "platformTypes", List.of("ANY_PLATFORM"),
                            "threatEntryTypes", List.of("URL"),
                            "threatEntries", List.of(Map.of("url", url))));
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(ENDPOINT + URLEncoder.encode(apiKey, StandardCharsets.UTF_8)))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Safe Browsing lookup returned HTTP {}; allowing URL", response.statusCode());
                return false;
            }
            JsonNode matches = objectMapper.readTree(response.body()).path("matches");
            return matches.isArray() && !matches.isEmpty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            log.warn("Safe Browsing lookup failed; allowing URL: {}", e.getMessage());
            return false;
        }
    }
}
