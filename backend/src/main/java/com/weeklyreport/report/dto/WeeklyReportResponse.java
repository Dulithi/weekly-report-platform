package com.weeklyreport.report.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.weeklyreport.report.ReportStatus;

public record WeeklyReportResponse(
        UUID id,
        UUID userId,
        LocalDate weekStart,
        LocalDate weekEnd,
        ReportStatus status,
        Instant submittedAt,
        Instant approvedAt,
        long entityVersion,
        ReportVersionResponse currentVersion
) {
}