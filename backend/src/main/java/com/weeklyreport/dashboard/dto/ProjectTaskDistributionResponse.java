package com.weeklyreport.dashboard.dto;

import java.util.UUID;

public record ProjectTaskDistributionResponse(
        UUID projectId,
        String projectName,
        long taskCount
) {
}
