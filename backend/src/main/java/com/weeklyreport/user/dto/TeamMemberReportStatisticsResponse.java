package com.weeklyreport.user.dto;

public record TeamMemberReportStatisticsResponse(
        long totalReports,
        long draftReports,
        long submittedReports,
        long needsCorrectionReports,
        long approvedReports
) {
}
