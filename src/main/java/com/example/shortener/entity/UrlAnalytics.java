package com.example.shortener.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * One recorded click on a short link. Linked to the link by id, so renaming
 * the alias keeps its history. Stores only privacy-reduced data: a truncated
 * IP and the referrer's origin.
 */
@Entity
@Table(name = "url_analytics", indexes = {
        @Index(name = "idx_analytics_url_time", columnList = "url_id, accessed_at"),
        @Index(name = "idx_analytics_accessed_at", columnList = "accessed_at")
})
public class UrlAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "url_id", nullable = false, foreignKey = @ForeignKey(name = "fk_analytics_url"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UrlEntity url;

    @Column(name = "accessed_at", nullable = false, updatable = false)
    private Instant accessedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "referer", length = 512)
    private String referer;

    protected UrlAnalytics() {}

    public UrlAnalytics(UrlEntity url, Instant accessedAt, String ipAddress, String userAgent, String referer) {
        this.url = url;
        this.accessedAt = accessedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.referer = referer;
    }

    public Long getId() { return id; }
    public UrlEntity getUrl() { return url; }
    public Instant getAccessedAt() { return accessedAt; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getReferer() { return referer; }
}
