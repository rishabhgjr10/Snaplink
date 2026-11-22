package com.example.shortener.repository;

import com.example.shortener.entity.UrlAnalytics;
import com.example.shortener.entity.UrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UrlAnalyticsRepository extends JpaRepository<UrlAnalytics, Long> {

    long countByUrl(UrlEntity url);

    List<UrlAnalytics> findTop10ByUrlOrderByAccessedAtDesc(UrlEntity url);

    @Query("select count(a) from UrlAnalytics a where a.url.ownerUid = :owner")
    long countByOwner(@Param("owner") String owner);

    /** Rows of [hour (0-23), count] for clicks at or after {@code since}. Timestamps are stored in UTC. */
    @Query("""
            select extract(hour from a.accessedAt), count(a)
            from UrlAnalytics a
            where a.url = :url and a.accessedAt >= :since
            group by extract(hour from a.accessedAt)
            """)
    List<Object[]> countByHourSince(@Param("url") UrlEntity url, @Param("since") Instant since);

    @Modifying
    @Query("delete from UrlAnalytics a where a.accessedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
