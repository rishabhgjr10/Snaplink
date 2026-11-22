package com.example.shortener.controller;

import com.example.shortener.dto.ReportRequest;
import com.example.shortener.security.CurrentUser;
import com.example.shortener.service.ReportService;
import com.example.shortener.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public abuse reporting (rate limited like any other anonymous write). */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUser currentUser;

    public ReportController(ReportService reportService, CurrentUser currentUser) {
        this.reportService = reportService;
        this.currentUser = currentUser;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> report(@Valid @RequestBody ReportRequest request) {
        reportService.file(request, currentUser.uid().orElse(null));
        return ResponseEntity.ok(ApiResponse.success(null, "Thank you. We will review this link.", 200));
    }
}
