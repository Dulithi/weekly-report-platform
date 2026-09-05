package com.weeklyreport.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        Jwt jwt,
        RefreshToken refreshToken,
        Cors cors
) {

    public record Jwt(
            String issuer,
            Duration accessTokenTtl
    ) {
    }

    public record RefreshToken(
            Duration ttl,
            String cookieName,
            boolean secure,
            String sameSite
    ) {
    }

    public record Cors(
            String allowedOrigin
    ) {
    }
}