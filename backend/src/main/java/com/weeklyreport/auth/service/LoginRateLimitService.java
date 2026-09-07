package com.weeklyreport.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.security.SecurityProperties;

@Service
public class LoginRateLimitService {
    private final JdbcTemplate jdbcTemplate;
    private final SecurityProperties properties;

    public LoginRateLimitService(JdbcTemplate jdbcTemplate, SecurityProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    // Commit the reservation before password verification, including when authentication fails.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Decision reserveAttempt(String email) {
        int limit = properties.loginRateLimit().maxAttempts();
        long seconds = properties.loginRateLimit().window().toSeconds();
        return jdbcTemplate.queryForObject("""
                INSERT INTO login_attempt_bucket (bucket_key, attempts, window_ends_at)
                VALUES (?, 1, statement_timestamp() + (? * interval '1 second'))
                ON CONFLICT (bucket_key) DO UPDATE SET
                    attempts = CASE
                        WHEN login_attempt_bucket.window_ends_at <= statement_timestamp() THEN 1
                        ELSE LEAST(login_attempt_bucket.attempts + 1, ? + 1)
                    END,
                    window_ends_at = CASE
                        WHEN login_attempt_bucket.window_ends_at <= statement_timestamp()
                            THEN EXCLUDED.window_ends_at
                        ELSE login_attempt_bucket.window_ends_at
                    END
                RETURNING attempts <= ? AS allowed,
                    GREATEST(1, CEIL(EXTRACT(EPOCH FROM
                        (window_ends_at - statement_timestamp()))))::bigint AS retry_after
                """, (result, row) -> new Decision(result.getBoolean("allowed"),
                        result.getLong("retry_after")), key(email), seconds, limit, limit);
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    public void cleanupExpired() {
        // Bound cleanup work and skip counters currently being updated by login requests.
        jdbcTemplate.update("""
                DELETE FROM login_attempt_bucket
                WHERE window_ends_at <= statement_timestamp() AND bucket_key IN (
                    SELECT bucket_key FROM login_attempt_bucket
                    WHERE window_ends_at <= statement_timestamp()
                    ORDER BY window_ends_at LIMIT 1000 FOR UPDATE SKIP LOCKED
                )
                """);
    }

    private String key(String email) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record Decision(boolean allowed, long retryAfterSeconds) { }
}
