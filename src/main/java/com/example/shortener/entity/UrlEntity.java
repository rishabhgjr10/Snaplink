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
}
