package com.example.shortener.service;

import com.example.shortener.dto.LinkSummary;
import com.example.shortener.dto.PageResponse;
import com.example.shortener.dto.UrlAnalyticsResponse;
import com.example.shortener.dto.UrlItemDto;
import com.example.shortener.dto.UrlShortenRequest;
import com.example.shortener.dto.UrlShortenResponse;
import com.example.shortener.dto.UrlUpdateRequest;
import com.example.shortener.dto.VisitDto;
import com.example.shortener.entity.LinkStatus;
import com.example.shortener.entity.UrlAnalytics;
import com.example.shortener.entity.UrlEntity;
import com.example.shortener.exception.ApiException;
import com.example.shortener.repository.UrlAnalyticsRepository;
import com.example.shortener.repository.UrlRepository;
import com.example.shortener.service.LifetimePolicy.Lifetime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Link management. Every read and write except anonymous creation and redirect
 * resolution is scoped to the owner's Firebase UID; another user's link behaves
 * exactly like a missing one (404).
 */
@Service
public class UrlService {

    static final int MAX_PAGE_SIZE = 100;
    private static final int GENERATE_ATTEMPTS = 5;
    private static final Set<String> STATUS_FILTERS = Set.of("ALL", "ACTIVE", "SCHEDULED", "EXPIRED");

    /** Outcome of looking up a short code for a redirect. */
    public record Resolution(LinkStatus status, String url, Instant startsAt) {}

    private final UrlRepository urlRepository;
    private final UrlAnalyticsRepository analyticsRepository;
    private final UrlCache cache;
    private final UrlPolicy policy;
    private final LifetimePolicy lifetimePolicy;
    private final ShortCodeGenerator codeGenerator;
    private final TransactionTemplate tx;
    private final Clock clock;

    public UrlService(UrlRepository urlRepository, UrlAnalyticsRepository analyticsRepository, UrlCache cache,
                      UrlPolicy policy, LifetimePolicy lifetimePolicy, ShortCodeGenerator codeGenerator,
                      TransactionTemplate tx, Clock clock) {
        this.urlRepository = urlRepository;
        this.analyticsRepository = analyticsRepository;
        this.cache = cache;
        this.policy = policy;
        this.lifetimePolicy = lifetimePolicy;
        this.codeGenerator = codeGenerator;
        this.tx = tx;
        this.clock = clock;
    }

    /**
     * Creates a link. {@code ownerUid} is null for anonymous creation.
     * Validation (which may call Safe Browsing) runs before any database work.
     */
    public UrlShortenResponse shorten(UrlShortenRequest request, String ownerUid) {
        Instant now = clock.instant();
        String destination = policy.checkDestination(request.originalUrl());
        Lifetime lifetime = lifetimePolicy.forNewLink(request.expiresAt(), request.startsAt(), request.maxClicks(),
                ownerUid == null, now);
        boolean custom = request.customAlias() != null && !request.customAlias().isBlank();
        String code = custom ? policy.checkAlias(request.customAlias()) : null;

        if (custom) {
            if (urlRepository.existsByShortCode(code)) {
                throw new ApiException.AliasConflict(code);
            }
        } else {
            code = freeGeneratedCode();
        }

        UrlEntity entity = new UrlEntity(destination, code, ownerUid);
        applyLifetime(entity, lifetime);
        try {
            urlRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            // Another request took the same code between the check and the insert.
            throw custom ? new ApiException.AliasConflict(code) : new ApiException.AliasConflict();
        }
        cacheIfRedirectable(entity, now);
        return new UrlShortenResponse(code, destination, entity.getExpiresAt(), entity.getStartsAt(), entity.getMaxClicks());
    }

    /**
     * @param status ALL, ACTIVE, SCHEDULED or EXPIRED (expired includes links at their click limit)
     * @param sort   "newest" or "expiring" (soonest expiry first)
     */
    @Transactional(readOnly = true)
    public PageResponse<UrlItemDto> list(String ownerUid, String query, String status, String sort, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        String pattern = "%" + escapeLike(query == null ? "" : query.trim().toLowerCase(Locale.ROOT)) + "%";
        String filter = status == null ? "ALL" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUS_FILTERS.contains(filter)) {
            throw new ApiException.InvalidInput("Unknown status filter. Use all, active, scheduled or expired.");
        }
        Instant now = clock.instant();
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        Page<UrlEntity> result = "expiring".equalsIgnoreCase(sort)
                ? urlRepository.findPageExpiringSoon(ownerUid, pattern, filter, now, pageable)
                : urlRepository.findPageNewest(ownerUid, pattern, filter, now, pageable);
        return PageResponse.of(result.map(e -> UrlItemDto.of(e, now)));
    }

    @Transactional(readOnly = true)
    public LinkSummary summary(String ownerUid) {
        Instant now = clock.instant();
        return new LinkSummary(
                urlRepository.countByOwnerUid(ownerUid),
                urlRepository.sumClicksByOwner(ownerUid),
                urlRepository.countActive(ownerUid, now),
                urlRepository.countExpired(ownerUid, now));
    }

    public UrlItemDto update(String ownerUid, String shortCode, UrlUpdateRequest request) {
        boolean newUrl = request.originalUrl() != null && !request.originalUrl().isBlank();
        boolean newAlias = request.customAlias() != null && !request.customAlias().isBlank();
        if (!newUrl && !newAlias && !request.changesLifetime()) {
            throw new ApiException.InvalidInput("Nothing to update.");
        }
        // Validate (possibly a network call) before opening a transaction.
        String destination = newUrl ? policy.checkDestination(request.originalUrl()) : null;
        String alias = newAlias ? policy.checkAlias(request.customAlias()) : null;

        try {
            return tx.execute(status -> {
                Instant now = clock.instant();
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
                if (request.changesLifetime()) {
                    applyLifetime(entity, lifetimeAfter(entity, request, now));
                }
                urlRepository.saveAndFlush(entity);

                // Applied only after commit (see UrlCache).
                cache.evict(oldCode);
                cacheIfRedirectable(entity, now);
                return UrlItemDto.of(entity, now);
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

    /** Deletes all of the caller's expired links (time or click limit). Returns how many were deleted. */
    @Transactional
    public int deleteExpired(String ownerUid) {
        List<UrlEntity> expired = urlRepository.findExpiredByOwner(ownerUid, clock.instant());
        if (expired.isEmpty()) {
            return 0;
        }
        urlRepository.deleteByIdsBulk(expired.stream().map(UrlEntity::getId).toList());
        expired.forEach(e -> cache.evict(e.getShortCode()));
        return expired.size();
    }

    @Transactional(readOnly = true)
    public UrlAnalyticsResponse analytics(String ownerUid, String shortCode) {
        UrlEntity entity = owned(ownerUid, shortCode);

        long[] byHour = new long[24];
        Instant startOfDay = LocalDate.now(clock.withZone(ZoneOffset.UTC)).atStartOfDay(ZoneOffset.UTC).toInstant();
        for (Object[] row : analyticsRepository.countByHourSince(entity, startOfDay)) {
            int hour = ((Number) row[0]).intValue();
            if (hour >= 0 && hour < 24) {
                byHour[hour] = ((Number) row[1]).longValue();
            }
        }

        List<VisitDto> recent = analyticsRepository.findTop10ByUrlOrderByAccessedAtDesc(entity).stream()
                .map(UrlService::toVisit)
                .toList();
        return new UrlAnalyticsResponse(entity.getClickCount(), byHour, recent);
    }

    /**
     * What a redirect for {@code shortCode} should do, or empty when the code is unknown.
     * Only links that redirect without further checks are cached: links with a start time
     * or a click limit always come from the database.
     */
    @Transactional(readOnly = true)
    public Optional<Resolution> resolve(String shortCode) {
        Instant now = clock.instant();
        Optional<UrlCache.CachedLink> cached = cache.get(shortCode);
        if (cached.isPresent()) {
            Instant expiresAt = cached.get().expiresAt();
            if (expiresAt == null || now.isBefore(expiresAt)) {
                return Optional.of(new Resolution(LinkStatus.ACTIVE, cached.get().url(), null));
            }
            cache.evict(shortCode);
        }
        Optional<UrlEntity> found = urlRepository.findByShortCode(shortCode);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        UrlEntity entity = found.get();
        LinkStatus status = entity.status(now);
        if (status.redirects()) {
            cacheIfRedirectable(entity, now);
        }
        return Optional.of(new Resolution(status, entity.getOriginalUrl(), entity.getStartsAt()));
    }

    /** Admin takedown or restore. */
    @Transactional
    public void setDisabled(String shortCode, boolean disabled) {
        UrlEntity entity = urlRepository.findByShortCode(shortCode).orElseThrow(ApiException.LinkNotFound::new);
        entity.setDisabled(disabled);
        urlRepository.save(entity);
        cache.evict(shortCode);
    }

    private Lifetime lifetimeAfter(UrlEntity entity, UrlUpdateRequest r, Instant now) {
        boolean expiryChanged = r.expiresAt() != null || Boolean.TRUE.equals(r.clearExpiresAt());
        boolean startChanged = r.startsAt() != null || Boolean.TRUE.equals(r.clearStartsAt());
        Instant expiresAt = Boolean.TRUE.equals(r.clearExpiresAt()) ? null
                : r.expiresAt() != null ? r.expiresAt() : entity.getExpiresAt();
        Instant startsAt = Boolean.TRUE.equals(r.clearStartsAt()) ? null
                : r.startsAt() != null ? r.startsAt() : entity.getStartsAt();
        Integer maxClicks = Boolean.TRUE.equals(r.clearMaxClicks()) ? null
                : r.maxClicks() != null ? r.maxClicks() : entity.getMaxClicks();
        return lifetimePolicy.forUpdate(expiresAt, expiryChanged, startsAt, startChanged, maxClicks, now);
    }

    private static void applyLifetime(UrlEntity entity, Lifetime lifetime) {
        entity.setExpiresAt(lifetime.expiresAt());
        entity.setStartsAt(lifetime.startsAt());
        entity.setMaxClicks(lifetime.maxClicks());
    }

    /** Caches links whose only limit is an expiry time (checked on every cache hit). */
    private void cacheIfRedirectable(UrlEntity entity, Instant now) {
        if (entity.status(now).redirects() && entity.getStartsAt() == null && entity.getMaxClicks() == null) {
            cache.put(entity.getShortCode(), entity.getOriginalUrl(), entity.getExpiresAt());
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
