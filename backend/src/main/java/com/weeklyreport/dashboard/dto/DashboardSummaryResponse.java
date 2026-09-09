package com.weeklyreport.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long totalActiveMembers,
        long submittedReports,
        long onTimeSubmissions,
        long pendingSubmissions,
        long lateSubmissions,
        BigDecimal submissionRatePercent,
        BigDecimal onTimeComplianceRatePercent,
        long needsCorrectionReports,
        long openBlockers
) {
}
