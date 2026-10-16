package com.example.shortener.dto;

import com.example.shortener.entity.UrlAnalytics;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class UrlAnalyticsResponse {
    private int totalClicks;
    private List<UrlAnalytics> recentVisits;

    public UrlAnalyticsResponse(int totalClicks, List<UrlAnalytics> recentVisits) {
        this.totalClicks = totalClicks;
        this.recentVisits = recentVisits;
    }

    public int getTotalClicks() { return totalClicks; }
    public List<UrlAnalytics> getRecentVisits() { return recentVisits; }
}
