package com.example.shortener.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** An abuse report about a short link, reviewed by an admin. */
@Entity
@Table(name = "link_reports", indexes = @Index(name = "idx_reports_status", columnList = "status, created_at"))
public class LinkReport {

    public enum Reason { PHISHING, MALWARE, SPAM, ILLEGAL, OTHER }

    public enum Status { OPEN, RESOLVED, DISMISSED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_code", nullable = false, length = 64)
    private String shortCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 32)
    private Reason reason;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "reporter_uid", length = 128)
    private String reporterUid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private Status status = Status.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LinkReport() {}

    public LinkReport(String shortCode, Reason reason, String details, String reporterUid) {
        this.shortCode = shortCode;
        this.reason = reason;
        this.details = details;
        this.reporterUid = reporterUid;
    }

    public Long getId() { return id; }
    public String getShortCode() { return shortCode; }
    public Reason getReason() { return reason; }
    public String getDetails() { return details; }
    public String getReporterUid() { return reporterUid; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
