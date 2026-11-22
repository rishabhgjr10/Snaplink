package com.example.shortener.dto;

import com.example.shortener.entity.LinkReport;

import java.time.Instant;

/** Admin view of a report, with the reported link's current destination and state. */
public record ReportDto(Long id, String shortCode, LinkReport.Reason reason, String details,
                        LinkReport.Status status, Instant createdAt,
                        String destination, Boolean linkDisabled) {
}
