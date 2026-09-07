package com.weeklyreport.report.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReportVersionResponse(
        UUID id,
        int versionNumber,
        String notes,
        Instant createdAt,
        Instant submittedAt,
        long entityVersion,
        List<CompletedTaskResponse> completedTasks,
        List<PlannedTaskResponse> plannedTasks,
        List<BlockerResponse> blockers,
        List<AchievementResponse> achievements,
        List<TimeEntryResponse> timeEntries
) {
}