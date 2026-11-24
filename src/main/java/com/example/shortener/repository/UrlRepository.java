package com.example.shortener.repository;

import com.example.shortener.entity.UrlEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<UrlEntity, Long> {

    // ── Status predicates, shared by the queries below (must match UrlEntity.status) ──
    String EXPIRED = "(u.expiresAt <= :now or (u.maxClicks is not null and u.clickCount >= u.maxClicks))";
    String NOT_EXPIRED = "((u.expiresAt is null or u.expiresAt > :now) and (u.maxClicks is null or u.clickCount < u.maxClicks))";
    String ACTIVE = "(u.disabled = false and " + NOT_EXPIRED + " and (u.startsAt is null or u.startsAt <= :now))";
    String SCHEDULED = "(u.disabled = false and " + NOT_EXPIRED + " and u.startsAt > :now)";

    /** Owner, search pattern ("%" matches all) and status filter (ALL, ACTIVE, SCHEDULED, EXPIRED). */
    String FILTER = " where u.ownerUid = :owner"
            + " and (lower(u.shortCode) like :pattern escape '!' or lower(u.originalUrl) like :pattern escape '!')"
            + " and (:status = 'ALL'"
            + " or (:status = 'ACTIVE' and " + ACTIVE + ")"
            + " or (:status = 'SCHEDULED' and " + SCHEDULED + ")"
            + " or (:status = 'EXPIRED' and " + EXPIRED + "))";

    Optional<UrlEntity> findByShortCode(String shortCode);

    Optional<UrlEntity> findByShortCodeAndOwnerUid(String shortCode, String ownerUid);

    boolean existsByShortCode(String shortCode);

    List<UrlEntity> findByShortCodeIn(Collection<String> shortCodes);

    @Query(value = "select u from UrlEntity u" + FILTER + " order by u.id desc",
            countQuery = "select count(u) from UrlEntity u" + FILTER)
    Page<UrlEntity> findPageNewest(@Param("owner") String owner, @Param("pattern") String pattern,
                                   @Param("status") String status, @Param("now") Instant now, Pageable pageable);

    /** Links with an expiry first, soonest first; then links without one, newest first. */
    @Query(value = "select u from UrlEntity u" + FILTER
            + " order by case when u.expiresAt is null then 1 else 0 end, u.expiresAt asc, u.id desc",
            countQuery = "select count(u) from UrlEntity u" + FILTER)
    Page<UrlEntity> findPageExpiringSoon(@Param("owner") String owner, @Param("pattern") String pattern,
                                         @Param("status") String status, @Param("now") Instant now, Pageable pageable);

    long countByOwnerUid(String ownerUid);

    @Query("select coalesce(sum(u.clickCount), 0) from UrlEntity u where u.ownerUid = :owner")
    long sumClicksByOwner(@Param("owner") String owner);

    @Query("select count(u) from UrlEntity u where u.ownerUid = :owner and " + ACTIVE)
    long countActive(@Param("owner") String owner, @Param("now") Instant now);

    @Query("select count(u) from UrlEntity u where u.ownerUid = :owner and " + EXPIRED)
    long countExpired(@Param("owner") String owner, @Param("now") Instant now);

    @Query("select u from UrlEntity u where u.ownerUid = :owner and " + EXPIRED)
    List<UrlEntity> findExpiredByOwner(@Param("owner") String owner, @Param("now") Instant now);

    /** Anonymous links that expired before {@code cutoff} (cleanup job). */
    @Query("select u from UrlEntity u where u.ownerUid is null and u.expiresAt < :cutoff")
    List<UrlEntity> findAnonymousExpiredBefore(@Param("cutoff") Instant cutoff, Pageable pageable);

    /** Bulk delete; the database cascades to the links' analytics rows (fk_analytics_url ON DELETE CASCADE). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UrlEntity u where u.id = :id")
    int deleteByIdBulk(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from UrlEntity u where u.id in :ids")
    int deleteByIdsBulk(@Param("ids") Collection<Long> ids);

    /** Atomic increment, safe when several clicks are recorded at once. */
    @Transactional
    @Modifying
    @Query("update UrlEntity u set u.clickCount = u.clickCount + 1 where u.id = :id")
    int incrementClickCount(@Param("id") Long id);
}
