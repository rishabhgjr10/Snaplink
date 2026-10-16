package com.example.shortener.repository;

import com.example.shortener.entity.UrlAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UrlAnalyticsRepository extends JpaRepository<UrlAnalytics, Long> {
    List<UrlAnalytics> findTop10ByShortCodeOrderByAccessedAtDesc(String shortCode);
    long countByShortCode(String shortCode);
    void deleteByShortCode(String shortCode);
}
