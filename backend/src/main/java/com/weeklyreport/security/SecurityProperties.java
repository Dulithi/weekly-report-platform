package com.weeklyreport.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        Jwt jwt,
        RefreshToken refreshToken,
        Cors cors,
        LoginRateLimit loginRateLimit
) {

    public record LoginRateLimit(int maxAttempts, Duration window) {
        public LoginRateLimit {
            if (maxAttempts < 1 || maxAttempts > 10000) {
                throw new IllegalArgumentException("Login max attempts must be between 1 and 10000");
            }
            if (window == null || window.compareTo(Duration.ofSeconds(1)) < 0
                    || window.compareTo(Duration.ofDays(1)) > 0 || window.getNano() != 0) {
                throw new IllegalArgumentException("Login window must be whole seconds between 1 second and 1 day");
            }
        }
    }

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
