package com.example.shortener.dto;

import java.util.List;

/**
 * @param clicksTodayByHour 24 counts, index = hour of the current UTC day
 */
public record UrlAnalyticsResponse(long totalClicks, long[] clicksTodayByHour, List<VisitDto> recentVisits) {
}
