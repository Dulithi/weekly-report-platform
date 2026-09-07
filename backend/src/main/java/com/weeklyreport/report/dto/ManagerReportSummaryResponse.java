package com.weeklyreport.report.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.weeklyreport.report.ReportStatus;

public record ManagerReportSummaryResponse(
        UUID id,
        ManagerReportMemberResponse member,
        LocalDate weekStart,
        LocalDate weekEnd,
        ReportStatus status,
        Instant submittedAt,
        Instant approvedAt,
        Instant updatedAt
) {
}
