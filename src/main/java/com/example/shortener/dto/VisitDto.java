package com.example.shortener.dto;

import java.time.Instant;

/**
 * One recent click as shown to the link owner. Deliberately excludes the IP address
 * and full user agent; {@code referrer} is the origin only.
 */
public record VisitDto(Instant visitedAt, String referrer, String device) {
}
