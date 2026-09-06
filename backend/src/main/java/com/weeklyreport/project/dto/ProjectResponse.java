package com.weeklyreport.project.dto;

import java.time.Instant;
import java.util.UUID;

import com.weeklyreport.project.ProjectStatus;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        ProjectStatus status,
        UUID createdBy,
        Instant createdAt,
        Instant updatedAt
) {
}