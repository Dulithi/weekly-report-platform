package com.weeklyreport.auth.dto;

import java.time.Instant;

public record AccessTokenResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt
) {
}