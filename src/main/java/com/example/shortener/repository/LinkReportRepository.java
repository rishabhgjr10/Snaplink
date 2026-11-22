package com.example.shortener.repository;

import com.example.shortener.entity.LinkReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LinkReportRepository extends JpaRepository<LinkReport, Long> {

    Page<LinkReport> findByStatusOrderByCreatedAtDesc(LinkReport.Status status, Pageable pageable);
}
