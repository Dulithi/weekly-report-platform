package com.weeklyreport.user.service;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.weeklyreport.common.exception.ConflictException;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;

@Component
public class AdminContinuityGuard {

    // A stable application-specific key serializes active-admin removal decisions.
    private static final long LOCK_KEY = 0x5745454B4C594144L;

    private final JdbcTemplate jdbcTemplate;

    public AdminContinuityGuard(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void ensureAnotherActiveAdminExists(User user) {
        if (!user.isActive() || user.getRole() != UserRole.ADMIN) {
            return;
        }

        acquireTransactionLock();

        Integer activeAdmins = jdbcTemplate.queryForObject(
                "select count(*) from app_user where role = 'ADMIN' and active",
                Integer.class
        );

        if (activeAdmins == null || activeAdmins <= 1) {
            throw new ConflictException("At least one active administrator is required");
        }
    }

    private void acquireTransactionLock() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.prepareStatement(
                    "select pg_advisory_xact_lock(?)")) {
                statement.setLong(1, LOCK_KEY);
                statement.execute();
                return null;
            }
        });
    }
}
