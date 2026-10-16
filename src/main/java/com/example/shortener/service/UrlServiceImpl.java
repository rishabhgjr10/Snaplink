package com.example.shortener.service;

import com.example.shortener.dto.UrlShortenRequest;
import com.example.shortener.dto.UrlShortenResponse;
import com.example.shortener.dto.UrlAnalyticsResponse;
import com.example.shortener.dto.UrlItemDto;
import com.example.shortener.dto.UrlUpdateRequest;
import com.example.shortener.entity.UrlEntity;
import com.example.shortener.entity.UrlAnalytics;
import com.example.shortener.repository.UrlRepository;
import com.example.shortener.repository.UrlAnalyticsRepository;
import com.example.shortener.util.Base62Util;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final UrlAnalyticsRepository analyticsRepository;
    private final CacheManager cacheManager;

    public UrlServiceImpl(UrlRepository urlRepository, UrlAnalyticsRepository analyticsRepository, CacheManager cacheManager) {
        this.urlRepository = urlRepository;
        this.analyticsRepository = analyticsRepository;
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional
    public UrlShortenResponse shortenUrl(UrlShortenRequest request, String baseUrl) {
        String shortCode;
        UrlEntity entity = new UrlEntity();
        entity.setOriginalUrl(request.getOriginalUrl());

        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            shortCode = request.getCustomAlias().trim();
            if (urlRepository.findByShortCode(shortCode).isPresent()) {
                throw new IllegalArgumentException("Custom alias '" + shortCode + "' is already in use. Please choose another.");
            }
            entity.setShortCode(shortCode);
            urlRepository.save(entity);
        } else {
            entity = urlRepository.save(entity);
            shortCode = Base62Util.encode(entity.getId());
            entity.setShortCode(shortCode);
            urlRepository.save(entity);
        }

        String shortUrl = baseUrl + "/" + shortCode;
        if (cacheManager.getCache("urlCache") != null) {
            cacheManager.getCache("urlCache").put(shortCode, request.getOriginalUrl());
        }
        return new UrlShortenResponse(shortCode, shortUrl, request.getOriginalUrl());
    }

    @Override
    public List<UrlItemDto> getAllUrls(String baseUrl) {
        List<UrlEntity> entities = urlRepository.findAllByOrderByIdDesc();
        return entities.stream().map(e -> {
            long clicks = analyticsRepository.countByShortCode(e.getShortCode());
            String shortUrl = baseUrl + "/" + e.getShortCode();
            return new UrlItemDto(e.getShortCode(), shortUrl, e.getOriginalUrl(), e.getCreatedAt(), clicks);
        }).toList();
    }

    @Override
    @Transactional
    public UrlItemDto updateUrl(String shortCode, UrlUpdateRequest request, String baseUrl) {
        UrlEntity entity = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new IllegalArgumentException("URL not found for short code: " + shortCode));

        String oldCode = entity.getShortCode();
        String newCode = oldCode;

        if (request.getOriginalUrl() != null && !request.getOriginalUrl().isBlank()) {
            entity.setOriginalUrl(request.getOriginalUrl().trim());
        }

        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank() && !request.getCustomAlias().trim().equals(oldCode)) {
            newCode = request.getCustomAlias().trim();
            if (urlRepository.findByShortCode(newCode).isPresent()) {
                throw new IllegalArgumentException("Custom alias '" + newCode + "' is already in use.");
            }
            entity.setShortCode(newCode);
        }

        urlRepository.save(entity);

        // Update cache
        if (cacheManager.getCache("urlCache") != null) {
            cacheManager.getCache("urlCache").evict(oldCode);
            cacheManager.getCache("urlCache").put(newCode, entity.getOriginalUrl());
        }

        long clicks = analyticsRepository.countByShortCode(newCode);
        String shortUrl = baseUrl + "/" + newCode;
        return new UrlItemDto(newCode, shortUrl, entity.getOriginalUrl(), entity.getCreatedAt(), clicks);
    }

    @Override
    @Transactional
    public void deleteUrl(String shortCode) {
        urlRepository.deleteByShortCode(shortCode);
        analyticsRepository.deleteByShortCode(shortCode);
        if (cacheManager.getCache("urlCache") != null) {
            cacheManager.getCache("urlCache").evict(shortCode);
        }
    }

    @Override
    public UrlAnalyticsResponse getAnalytics(String shortCode) {
        long total = analyticsRepository.countByShortCode(shortCode);
        List<UrlAnalytics> recent = analyticsRepository.findTop10ByShortCodeOrderByAccessedAtDesc(shortCode);
        return new UrlAnalyticsResponse((int) total, recent);
    }

    @Override
    public Optional<String> resolveOriginalUrl(String shortCode) {
        // Try cache first
        if (cacheManager.getCache("urlCache") != null) {
            org.springframework.cache.Cache.ValueWrapper wrapper = cacheManager.getCache("urlCache").get(shortCode);
            if (wrapper != null) {
                return Optional.ofNullable((String) wrapper.get());
            }
        }
        // Fallback to DB
        return urlRepository.findByShortCode(shortCode).map(UrlEntity::getOriginalUrl);
    }

    @Override
    @Transactional
    public void recordClick(String shortCode, String ipAddress, String userAgent, String referer) {
        UrlAnalytics analytics = new UrlAnalytics(shortCode, ipAddress, userAgent, referer);
        analyticsRepository.save(analytics);
    }
}
