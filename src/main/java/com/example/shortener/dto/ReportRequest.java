package com.example.shortener.dto;

import com.example.shortener.entity.LinkReport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotBlank(message = "Enter the short code or link you are reporting.")
        @Size(max = 64, message = "Short code must be at most 64 characters.")
        String shortCode,

        @NotNull(message = "Choose a reason.")
        LinkReport.Reason reason,

        @Size(max = 1000, message = "Details must be at most 1000 characters.")
        String details) {
}
