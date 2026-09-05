package com.weeklyreport.repository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

import jakarta.transaction.Transactional;

@SpringBootTest
@Transactional
class UserRepositoryIntegrationTest extends PostgresIntegrationTest{

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldSaveAndFindUserByEmailIgnoringCase() {
        User user = new User(
                "dulithi@example.com",
                "test-hash",
                "Dulithi",
                "Jayasooriya",
                UserRole.TEAM_MEMBER
        );

        userRepository.saveAndFlush(user);

        var result = userRepository.findByEmailIgnoreCase(
                "DULITHI@EXAMPLE.COM"
        );

        assertTrue(result.isPresent());
        assertEquals(user.getId(), result.get().getId());
        assertEquals("dulithi@example.com", result.get().getEmail());
    }

    @Test
    void shouldRejectDuplicateEmailIgnoringCase() {
        User firstUser = new User(
                "member@example.com",
                "test-hash",
                "First",
                "Member",
                UserRole.TEAM_MEMBER
        );

        userRepository.saveAndFlush(firstUser);

        User duplicateUser = new User(
                "MEMBER@example.com",
                "another-test-hash",
                "Second",
                "Member",
                UserRole.TEAM_MEMBER
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(duplicateUser)
        );
    }

    @Test
    void shouldRejectInvalidRoleAtDatabaseLevel() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO app_user (
                            id,
                            email,
                            password_hash,
                            first_name,
                            last_name,
                            role,
                            active
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
                        UUID.randomUUID(),
                        "invalid-role@example.com",
                        "test-hash",
                        "Invalid",
                        "Role",
                        "SUPER_ADMIN",
                        true
                )
        );
    }
    
}
