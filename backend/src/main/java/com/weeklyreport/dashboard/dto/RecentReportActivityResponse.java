package com.weeklyreport.dashboard.dto;

import java.time.Instant;
import java.util.UUID;

import com.weeklyreport.activity.ActivityType;

public record RecentReportActivityResponse(
        UUID id,
        ActivityType type,
        UUID reportId,
        DashboardActivityActorResponse actor,
        Instant createdAt
) {
}
