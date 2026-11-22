package com.example.shortener.service;

import com.example.shortener.dto.PageResponse;
import com.example.shortener.dto.ReportDto;
import com.example.shortener.dto.ReportRequest;
import com.example.shortener.entity.LinkReport;
import com.example.shortener.entity.UrlEntity;
import com.example.shortener.exception.ApiException;
import com.example.shortener.repository.LinkReportRepository;
import com.example.shortener.repository.UrlRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Abuse reports: anyone can file one; admins review them and disable links. */
@Service
public class ReportService {

    private final LinkReportRepository reportRepository;
    private final UrlRepository urlRepository;
    private final UrlService urlService;

    public ReportService(LinkReportRepository reportRepository, UrlRepository urlRepository, UrlService urlService) {
        this.reportRepository = reportRepository;
        this.urlRepository = urlRepository;
        this.urlService = urlService;
    }

    @Transactional
    public void file(ReportRequest request, String reporterUid) {
        String code = request.shortCode().trim();
        if (!urlRepository.existsByShortCode(code)) {
            throw new ApiException.LinkNotFound();
        }
        String details = request.details() == null || request.details().isBlank() ? null : request.details().trim();
        reportRepository.save(new LinkReport(code, request.reason(), details, reporterUid));
    }

    @Transactional(readOnly = true)
    public PageResponse<ReportDto> list(LinkReport.Status status, int page, int size) {
        Page<LinkReport> reports = reportRepository.findByStatusOrderByCreatedAtDesc(
                status, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        Map<String, UrlEntity> links = urlRepository
                .findByShortCodeIn(reports.getContent().stream().map(LinkReport::getShortCode).distinct().toList())
                .stream().collect(Collectors.toMap(UrlEntity::getShortCode, Function.identity()));
        return PageResponse.of(reports.map(r -> {
            UrlEntity link = links.get(r.getShortCode());
            return new ReportDto(r.getId(), r.getShortCode(), r.getReason(), r.getDetails(), r.getStatus(),
                    r.getCreatedAt(), link == null ? null : link.getOriginalUrl(),
                    link == null ? null : link.isDisabled());
        }));
    }

    /** Resolves a report; {@code disableLink} also takes the reported link down. */
    @Transactional
    public void resolve(Long reportId, LinkReport.Status status, boolean disableLink) {
        if (status == LinkReport.Status.OPEN) {
            throw new ApiException.InvalidInput("Choose RESOLVED or DISMISSED.");
        }
        LinkReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ApiException.InvalidInput("Report not found."));
        report.setStatus(status);
        reportRepository.save(report);
        if (disableLink && urlRepository.existsByShortCode(report.getShortCode())) {
            urlService.setDisabled(report.getShortCode(), true);
        }
    }
}
