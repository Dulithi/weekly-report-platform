package com.weeklyreport.user.dto;

import java.time.Instant;
import java.util.UUID;

public record ManagerAssignmentResponse(
        UUID teamMemberId,
        UUID managerId,
        Instant assignedAt
) {
}
