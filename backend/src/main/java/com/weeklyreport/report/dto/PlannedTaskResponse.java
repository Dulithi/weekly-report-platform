package com.weeklyreport.report.dto;

import java.util.UUID;

import com.weeklyreport.report.TaskPriority;

public record PlannedTaskResponse(
        UUID id,
        UUID projectId,
        String projectName,
        String taskName,
        String description,
        TaskPriority priority,
        Integer estimatedMinutes,
        int sortOrder
) {
}