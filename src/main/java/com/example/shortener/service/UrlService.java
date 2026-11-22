package com.example.shortener.service;

import com.example.shortener.dto.LinkSummary;
import com.example.shortener.dto.PageResponse;
import com.example.shortener.dto.UrlAnalyticsResponse;
import com.example.shortener.dto.UrlItemDto;
import com.example.shortener.dto.UrlShortenRequest;
import com.example.shortener.dto.UrlShortenResponse;
import com.example.shortener.dto.UrlUpdateRequest;
import com.example.shortener.dto.VisitDto;
import com.example.shortener.entity.UrlAnalytics;
import com.example.shortener.entity.UrlEntity;
import com.example.shortener.exception.ApiException;
import com.example.shortener.repository.UrlAnalyticsRepository;
import com.example.shortener.repository.UrlRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Link management. Every read and write except anonymous creation and redirect
 * resolution is scoped to the owner's Firebase UID; another user's link behaves
 * exactly like a missing one (404).
 */
@Service
public class UrlService {

    static final int MAX_PAGE_SIZE = 100;
    private static final int GENERATE_ATTEMPTS = 5;

    private final UrlRepository urlRepository;
    private final UrlAnalyticsRepository analyticsRepository;
    private final UrlCache cache;
    private final UrlPolicy policy;
    private final ShortCodeGenerator codeGenerator;
    private final TransactionTemplate tx;

    public UrlService(UrlRepository urlRepository, UrlAnalyticsRepository analyticsRepository, UrlCache cache,
                      UrlPolicy policy, ShortCodeGenerator codeGenerator, TransactionTemplate tx) {
        this.urlRepository = urlRepository;
        this.analyticsRepository = analyticsRepository;
        this.cache = cache;
        this.policy = policy;
        this.codeGenerator = codeGenerator;
        this.tx = tx;
    }

    /**
     * Creates a link. {@code ownerUid} is null for anonymous creation.
     * Validation (which may call Safe Browsing) runs before any database work.
     */
    public UrlShortenResponse shorten(UrlShortenRequest request, String ownerUid) {
        String destination = policy.checkDestination(request.originalUrl());
        boolean custom = request.customAlias() != null && !request.customAlias().isBlank();
        String code = custom ? policy.checkAlias(request.customAlias()) : null;

        if (custom) {
            if (urlRepository.existsByShortCode(code)) {
                throw new ApiException.AliasConflict(code);
            }
        } else {
            code = freeGeneratedCode();
        }

        try {
            urlRepository.saveAndFlush(new UrlEntity(destination, code, ownerUid));
        } catch (DataIntegrityViolationException e) {
            // Another request took the same code between the check and the insert.
            throw custom ? new ApiException.AliasConflict(code) : new ApiException.AliasConflict();
        }
        cache.put(code, destination);
        return new UrlShortenResponse(code, destination);
    }

    @Transactional(readOnly = true)
    public PageResponse<UrlItemDto> list(String ownerUid, String query, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        String pattern = "%" + escapeLike(query == null ? "" : query.trim().toLowerCase(Locale.ROOT)) + "%";
        return PageResponse.of(urlRepository.findPageWithClicks(ownerUid, pattern, PageRequest.of(safePage, safeSize)));
    }

    @Transactional(readOnly = true)
    public LinkSummary summary(String ownerUid) {
        return new LinkSummary(urlRepository.countByOwnerUid(ownerUid), analyticsRepository.countByOwner(ownerUid));
    }

    public UrlItemDto update(String ownerUid, String shortCode, UrlUpdateRequest request) {
        boolean newUrl = request.originalUrl() != null && !request.originalUrl().isBlank();
        boolean newAlias = request.customAlias() != null && !request.customAlias().isBlank();
        if (!newUrl && !newAlias) {
            throw new ApiException.InvalidInput("Nothing to update.");
        }
        // Validate (possibly a network call) before opening a transaction.
        String destination = newUrl ? policy.checkDestination(request.originalUrl()) : null;
        String alias = newAlias ? policy.checkAlias(request.customAlias()) : null;

        try {
            return tx.execute(status -> {
                UrlEntity entity = owned(ownerUid, shortCode);
                String oldCode = entity.getShortCode();
                if (destination != null) {
                    entity.setOriginalUrl(destination);
                }
                if (alias != null && !alias.equals(oldCode)) {
                    if (urlRepository.existsByShortCode(alias)) {
                        throw new ApiException.AliasConflict(alias);
                    }
                    entity.setShortCode(alias);
                }
                urlRepository.saveAndFlush(entity);

                // Applied only after commit (see UrlCache).
                cache.evict(oldCode);
                if (!entity.isDisabled()) {
                    cache.put(entity.getShortCode(), entity.getOriginalUrl());
                }
                return new UrlItemDto(entity.getShortCode(), entity.getOriginalUrl(), entity.getCreatedAt(),
                        analyticsRepository.countByUrl(entity), entity.isDisabled());
            });
        } catch (DataIntegrityViolationException e) {
            throw alias != null ? new ApiException.AliasConflict(alias) : new ApiException.AliasConflict();
        }
    }

    @Transactional
    public void delete(String ownerUid, String shortCode) {
        UrlEntity entity = owned(ownerUid, shortCode);
        urlRepository.deleteByIdBulk(entity.getId()); // analytics rows are removed by ON DELETE CASCADE
        cache.evict(shortCode);
    }

    @Transactional(readOnly = true)
    public UrlAnalyticsResponse analytics(String ownerUid, String shortCode) {
        UrlEntity entity = owned(ownerUid, shortCode);
        long total = analyticsRepository.countByUrl(entity);

        long[] byHour = new long[24];
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        for (Object[] row : analyticsRepository.countByHourSince(entity, startOfDay)) {
            int hour = ((Number) row[0]).intValue();
            if (hour >= 0 && hour < 24) {
                byHour[hour] = ((Number) row[1]).longValue();
            }
        }

        List<VisitDto> recent = analyticsRepository.findTop10ByUrlOrderByAccessedAtDesc(entity).stream()
                .map(UrlService::toVisit)
                .toList();
        return new UrlAnalyticsResponse(total, byHour, recent);
    }

    /**
     * Destination for a redirect, or empty when the code is unknown.
     * Throws {@link LinkGone} for links disabled by an admin.
     */
    @Transactional(readOnly = true)
    public Optional<String> resolve(String shortCode) {
        Optional<String> cached = cache.get(shortCode);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<UrlEntity> entity = urlRepository.findByShortCode(shortCode);
        if (entity.isEmpty()) {
            return Optional.empty();
        }
        if (entity.get().isDisabled()) {
            throw new LinkGone();
        }
        cache.put(shortCode, entity.get().getOriginalUrl());
        return Optional.of(entity.get().getOriginalUrl());
    }

    /** Admin takedown or restore. */
    @Transactional
    public void setDisabled(String shortCode, boolean disabled) {
        UrlEntity entity = urlRepository.findByShortCode(shortCode).orElseThrow(ApiException.LinkNotFound::new);
        entity.setDisabled(disabled);
        urlRepository.save(entity);
        cache.evict(shortCode);
    }

    /** Thrown by {@link #resolve} for a link that was taken down. */
    public static class LinkGone extends RuntimeException {
        public LinkGone() {
            super("This link has been disabled.");
        }
    }

    private UrlEntity owned(String ownerUid, String shortCode) {
        return urlRepository.findByShortCodeAndOwnerUid(shortCode, ownerUid)
                .orElseThrow(ApiException.LinkNotFound::new);
    }

    private String freeGeneratedCode() {
        for (int i = 0; i < GENERATE_ATTEMPTS; i++) {
            String code = codeGenerator.next();
            if (!urlRepository.existsByShortCode(code)) {
                return code;
            }
        }
        throw new ApiException.AliasConflict();
    }

    private static VisitDto toVisit(UrlAnalytics a) {
        return new VisitDto(a.getAccessedAt(), a.getReferer(), UserAgents.device(a.getUserAgent()));
    }

    /** Escapes LIKE wildcards with '!' (the repository query declares {@code escape '!'}). */
    private static String escapeLike(String s) {
        return s.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
