package com.weeklyreport.report.dto;

import java.util.UUID;

import com.weeklyreport.report.TaskPriority;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlannedTaskRequest(

        UUID projectId,

        @NotBlank
        @Size(max = 255)
        String taskName,

        @Size(max = 2000)
        String description,

        @NotNull
        TaskPriority priority,

        @Min(0)
        Integer estimatedMinutes
) {
}