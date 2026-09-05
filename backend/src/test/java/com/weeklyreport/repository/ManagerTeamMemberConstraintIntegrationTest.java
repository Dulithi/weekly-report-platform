package com.weeklyreport.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.ManagerTeamMember;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.ManagerTeamMemberRepository;
import com.weeklyreport.user.repository.UserRepository;

@SpringBootTest
@Transactional
class ManagerTeamMemberConstraintIntegrationTest
        extends PostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ManagerTeamMemberRepository managerTeamMemberRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldAssignTeamMemberToManager() {
        User manager = createUser(
                "manager@example.com",
                UserRole.MANAGER
        );

        User member = createUser(
                "member@example.com",
                UserRole.TEAM_MEMBER
        );

        ManagerTeamMember assignment =
                new ManagerTeamMember(member, manager);

        managerTeamMemberRepository.saveAndFlush(assignment);

        var result =
                managerTeamMemberRepository
                        .findByTeamMemberId(member.getId());

        assertTrue(result.isPresent());

        assertEquals(
                manager.getId(),
                result.get().getManager().getId()
        );
    }

    @Test
    void shouldRejectManagerAssignedToThemselves() {
        User manager = createUser(
                "self-manager@example.com",
                UserRole.MANAGER
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        INSERT INTO manager_team_member (
                            team_member_id,
                            manager_id
                        )
                        VALUES (?, ?)
                        """,
                        manager.getId(),
                        manager.getId()
                )
        );
    }

    private User createUser(
            String email,
            UserRole role
    ) {
        User user = new User(
                email,
                "test-hash",
                "Test",
                "User",
                role
        );

        return userRepository.saveAndFlush(user);
    }
}