package com.weeklyreport.report.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.weeklyreport.report.ReportStatus;

public record WeeklyReportSummaryResponse(
        UUID id,
        LocalDate weekStart,
        LocalDate weekEnd,
        ReportStatus status,
        int currentVersionNumber,
        Instant submittedAt,
        Instant updatedAt
) {
}