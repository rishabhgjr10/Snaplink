package com.example.shortener.controller;

import com.example.shortener.dto.PageResponse;
import com.example.shortener.dto.ReportDto;
import com.example.shortener.entity.LinkReport;
import com.example.shortener.security.CurrentUser;
import com.example.shortener.service.ReportService;
import com.example.shortener.service.UrlService;
import com.example.shortener.util.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Moderation endpoints; callers must be listed in {@code app.admin-uids}. */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final ReportService reportService;
    private final UrlService urlService;
    private final CurrentUser currentUser;

    public AdminController(ReportService reportService, UrlService urlService, CurrentUser currentUser) {
        this.reportService = reportService;
        this.urlService = urlService;
        this.currentUser = currentUser;
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<PageResponse<ReportDto>>> reports(
            @RequestParam(value = "status", defaultValue = "OPEN") LinkReport.Status status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "25") int size) {
        currentUser.requireAdmin();
        return ResponseEntity.ok(ApiResponse.success(reportService.list(status, page, size), "Reports", 200));
    }

    @PostMapping("/reports/{id}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolveReport(
            @PathVariable("id") Long id,
            @RequestParam("status") LinkReport.Status status,
            @RequestParam(value = "disableLink", defaultValue = "false") boolean disableLink) {
        currentUser.requireAdmin();
        reportService.resolve(id, status, disableLink);
        return ResponseEntity.ok(ApiResponse.success(null, "Report updated", 200));
    }

    @PostMapping("/urls/{shortCode}/disable")
    public ResponseEntity<ApiResponse<Void>> disable(@PathVariable("shortCode") String shortCode) {
        currentUser.requireAdmin();
        urlService.setDisabled(shortCode, true);
        return ResponseEntity.ok(ApiResponse.success(null, "Link disabled", 200));
    }

    @PostMapping("/urls/{shortCode}/enable")
    public ResponseEntity<ApiResponse<Void>> enable(@PathVariable("shortCode") String shortCode) {
        currentUser.requireAdmin();
        urlService.setDisabled(shortCode, false);
        return ResponseEntity.ok(ApiResponse.success(null, "Link enabled", 200));
    }
}
