package com.weeklyreport.dashboard.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.weeklyreport.report.dto.SubmissionTrackingResponse;

public record ManagerDashboardResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        Instant dueAt,
        DashboardSummaryResponse summary,
        List<SubmissionTrackingResponse> submissionsByMember,
        List<CompletedTaskTrendPointResponse> completedTaskTrend,
        List<ProjectTaskDistributionResponse> projectTaskDistribution,
        List<TaskTypeTimeResponse> timeByTaskType,
        List<RecentReportActivityResponse> recentActivity
) {
}
