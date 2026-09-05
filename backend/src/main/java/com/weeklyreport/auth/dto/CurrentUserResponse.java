package com.weeklyreport.auth.dto;

import java.util.UUID;

import com.weeklyreport.user.UserRole;

public record CurrentUserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        UserRole role
) {
}
