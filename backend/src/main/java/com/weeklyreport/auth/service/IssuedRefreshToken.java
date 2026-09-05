package com.weeklyreport.auth.service;

import java.time.Instant;

public record IssuedRefreshToken(
        String rawToken,
        Instant expiresAt
) {
}