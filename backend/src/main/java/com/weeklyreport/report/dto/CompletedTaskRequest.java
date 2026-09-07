package com.weeklyreport.report.dto;

import java.util.UUID;

import com.weeklyreport.report.TaskPriority;
import com.weeklyreport.report.TaskStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompletedTaskRequest(

        UUID projectId,

        @NotBlank
        @Size(max = 255)
        String taskName,

        @Size(max = 2000)
        String description,

        @NotNull
        TaskPriority priority,

        @Min(0)
        @Max(100)
        int plannedPercentage,

        @Min(0)
        @Max(100)
        int actualPercentage,

        @NotNull
        TaskStatus status,

        @Min(0)
        Integer plannedMinutes,

        @Min(0)
        Integer spentMinutes,

        @Size(max = 4000)
        String deliverable
) {
}