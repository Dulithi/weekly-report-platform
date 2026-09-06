package com.weeklyreport.user.dto;

import java.util.UUID;

public record TeamMemberResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        boolean active
) {
}
