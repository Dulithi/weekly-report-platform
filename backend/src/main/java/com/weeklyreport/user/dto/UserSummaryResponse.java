package com.weeklyreport.user.dto;

import java.util.UUID;

import com.weeklyreport.user.UserRole;

public record UserSummaryResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        boolean active
) {
}
