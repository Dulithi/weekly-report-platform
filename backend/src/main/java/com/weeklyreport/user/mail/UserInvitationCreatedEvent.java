package com.weeklyreport.user.mail;

import java.time.Instant;
import java.util.UUID;

import com.weeklyreport.user.UserRole;

public record UserInvitationCreatedEvent(
        UUID invitationId,
        String recipient,
        UserRole role,
        Instant expiresAt,
        String acceptanceToken
) {
}
