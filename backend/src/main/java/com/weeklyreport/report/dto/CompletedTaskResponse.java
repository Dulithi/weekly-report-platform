package com.weeklyreport.report.dto;

import java.util.UUID;

import com.weeklyreport.report.TaskPriority;
import com.weeklyreport.report.TaskStatus;

public record CompletedTaskResponse(
        UUID id,
        UUID projectId,
        String projectName,
        String taskName,
        String description,
        TaskPriority priority,
        int plannedPercentage,
        int actualPercentage,
        TaskStatus status,
        Integer plannedMinutes,
        Integer spentMinutes,
        String deliverable,
        int sortOrder
) {
}