package com.weeklyreport.assistant.service;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.assistant.config.AssistantProperties;
import com.weeklyreport.assistant.exception.AssistantRateLimitExceededException;

@Service
public class AssistantRateLimitService {
    private final JdbcTemplate jdbcTemplate;
    private final AssistantProperties properties;

    public AssistantRateLimitService(JdbcTemplate jdbcTemplate, AssistantProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reserve(UUID accountId) {
        int limit = properties.rateLimit().maxRequests();
        long seconds = properties.rateLimit().window().toSeconds();
        Decision decision = jdbcTemplate.queryForObject("""
                INSERT INTO assistant_request_bucket (account_key, attempts, window_ends_at)
                VALUES (?, 1, statement_timestamp() + (? * interval '1 second'))
                ON CONFLICT (account_key) DO UPDATE SET
                    attempts = CASE
                        WHEN assistant_request_bucket.window_ends_at <= statement_timestamp() THEN 1
                        ELSE LEAST(assistant_request_bucket.attempts + 1, ? + 1)
                    END,
                    window_ends_at = CASE
                        WHEN assistant_request_bucket.window_ends_at <= statement_timestamp()
                            THEN EXCLUDED.window_ends_at
                        ELSE assistant_request_bucket.window_ends_at
                    END
                RETURNING attempts <= ? AS allowed,
                    GREATEST(1, CEIL(EXTRACT(EPOCH FROM
                        (window_ends_at - statement_timestamp()))))::bigint AS retry_after
                """, (result, row) -> new Decision(
                        result.getBoolean("allowed"), result.getLong("retry_after")
                ), accountId.toString(), seconds, limit, limit);
        if (!decision.allowed()) {
            throw new AssistantRateLimitExceededException(decision.retryAfterSeconds());
        }
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    public void cleanupExpired() {
        jdbcTemplate.update("""
                DELETE FROM assistant_request_bucket
                WHERE window_ends_at <= statement_timestamp() AND account_key IN (
                    SELECT account_key FROM assistant_request_bucket
                    WHERE window_ends_at <= statement_timestamp()
                    ORDER BY window_ends_at LIMIT 1000 FOR UPDATE SKIP LOCKED
                )
                """);
    }

    private record Decision(boolean allowed, long retryAfterSeconds) {
    }
}
