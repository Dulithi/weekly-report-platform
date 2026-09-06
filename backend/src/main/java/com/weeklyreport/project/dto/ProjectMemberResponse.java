package com.weeklyreport.project.dto;

import java.util.UUID;

import com.weeklyreport.user.UserRole;

public record ProjectMemberResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UserRole role
) {
}