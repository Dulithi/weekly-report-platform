package com.weeklyreport.project.dto;

import com.weeklyreport.project.ProjectStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateProjectStatusRequest(
        @NotNull ProjectStatus status
) {
}
