package com.example.shortener.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "url_mapping",
        uniqueConstraints = @UniqueConstraint(name = "uk_url_mapping_short_code", columnNames = "short_code"),
        indexes = @Index(name = "idx_url_mapping_owner", columnList = "owner_uid, id"))
public class UrlEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    /** Case-sensitive (utf8mb4_bin in MySQL, see V2 migration). */
    @Column(name = "short_code", nullable = false, length = 64)
    private String shortCode;

    /** Firebase UID of the creator; null for links created anonymously. */
    @Column(name = "owner_uid", length = 128)
    private String ownerUid;

    /** Set by an admin takedown; disabled links answer 410 Gone. */
    @Column(name = "disabled", nullable = false)
    private boolean disabled;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** After this moment the link stops redirecting; null = never expires. */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /** Before this moment the link does not redirect yet; null = active immediately. */
    @Column(name = "starts_at")
    private Instant startsAt;

    /** The link stops redirecting once {@link #clickCount} reaches this; null = unlimited. */
    @Column(name = "max_clicks")
    private Integer maxClicks;

    /** Lifetime count of human clicks (bots and repeat clicks excluded). */
    @Column(name = "click_count", nullable = false)
    private long clickCount;

    public UrlEntity() {}

    public UrlEntity(String originalUrl, String shortCode, String ownerUid) {
        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.ownerUid = ownerUid;
    }

    public Long getId() { return id; }
    public String getOriginalUrl() { return originalUrl; }
    public void setOriginalUrl(String originalUrl) { this.originalUrl = originalUrl; }
    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getOwnerUid() { return ownerUid; }
    public boolean isDisabled() { return disabled; }
    public void setDisabled(boolean disabled) { this.disabled = disabled; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public Integer getMaxClicks() { return maxClicks; }
    public void setMaxClicks(Integer maxClicks) { this.maxClicks = maxClicks; }
    public long getClickCount() { return clickCount; }

    /** Status at {@code now}. A disabled link reports DISABLED regardless of its other limits. */
    public LinkStatus status(Instant now) {
        if (disabled) {
            return LinkStatus.DISABLED;
        }
        if (expiresAt != null && !now.isBefore(expiresAt)) {
            return LinkStatus.EXPIRED;
        }
        if (maxClicks != null && clickCount >= maxClicks) {
            return LinkStatus.LIMIT_REACHED;
        }
        if (startsAt != null && now.isBefore(startsAt)) {
            return LinkStatus.SCHEDULED;
        }
        return LinkStatus.ACTIVE;
    }
}
