package com.weeklyreport.user.dto;

import java.time.Instant;
import java.util.UUID;

import com.weeklyreport.user.InvitationStatus;
import com.weeklyreport.user.UserRole;

public record UserInvitationResponse(
        UUID id,
        String email,
        UserRole role,
        InvitationStatus status,
        Instant expiresAt,
        Instant acceptedAt,
        Instant revokedAt,
        Instant createdAt
) {
}
