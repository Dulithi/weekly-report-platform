package com.weeklyreport.report.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.weeklyreport.report.ReportStatus;

public record ManagerReportFilter(
        UUID memberId,
        UUID projectId,
        LocalDate from,
        LocalDate to,
        ReportStatus status
) {
}