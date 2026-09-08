package com.weeklyreport.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.auth.dto.LoginRequest;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.entity.ManagerTeamMember;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.ManagerTeamMemberRepository;
import com.weeklyreport.user.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ManagerAssignmentIntegrationTest
        extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ManagerTeamMemberRepository managerTeamMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminShouldAssignManagerToTeamMember()
            throws Exception {

        User admin = createUser(
                "manager-admin@example.com",
                UserRole.ADMIN
        );

        User manager = createUser(
                "manager-one@example.com",
                UserRole.MANAGER
        );

        User member = createUser(
                "assigned-member@example.com",
                UserRole.TEAM_MEMBER
        );

        String token = login(admin);

        assignManager(
                token,
                member.getId(),
                manager.getId()
        );

        ManagerTeamMember assignment =
                managerTeamMemberRepository
                        .findByTeamMemberId(
                                member.getId()
                        )
                        .orElseThrow();

        assertThat(
                assignment.getManager().getId()
        ).isEqualTo(
                manager.getId()
        );

        mockMvc.perform(
                        get("/api/v1/admin/manager-assignments")
                                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teamMemberId").value(member.getId().toString()))
                .andExpect(jsonPath("$[0].managerId").value(manager.getId().toString()))
                .andExpect(jsonPath("$[0].assignedAt").isNotEmpty());
    }

    @Test
    void assigningNewManagerShouldReplaceExistingManager()
            throws Exception {

        User admin = createUser(
                "replace-admin@example.com",
                UserRole.ADMIN
        );

        User managerOne = createUser(
                "manager-old@example.com",
                UserRole.MANAGER
        );

        User managerTwo = createUser(
                "manager-new@example.com",
                UserRole.MANAGER
        );

        User member = createUser(
                "replace-member@example.com",
                UserRole.TEAM_MEMBER
        );

        String token = login(admin);

        assignManager(
                token,
                member.getId(),
                managerOne.getId()
        );

        assignManager(
                token,
                member.getId(),
                managerTwo.getId()
        );

        ManagerTeamMember assignment =
                managerTeamMemberRepository
                        .findByTeamMemberId(
                                member.getId()
                        )
                        .orElseThrow();

        assertThat(
                assignment.getManager().getId()
        ).isEqualTo(
                managerTwo.getId()
        );
    }

    @Test
    void teamMemberCannotBeUsedAsManager()
            throws Exception {

        User admin = createUser(
                "invalid-manager-admin@example.com",
                UserRole.ADMIN
        );

        User fakeManager = createUser(
                "fake-manager@example.com",
                UserRole.TEAM_MEMBER
        );

        User member = createUser(
                "invalid-manager-member@example.com",
                UserRole.TEAM_MEMBER
        );

        String token = login(admin);

        mockMvc.perform(
                        put(
                                "/api/v1/admin/users/{teamMemberId}/manager",
                                member.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "managerId": "%s"
                                        }
                                        """.formatted(
                                                fakeManager.getId()
                                        )
                                )
                )
                .andExpect(status().isConflict());
    }

    @Test
    void managerCannotBeAssignedAsTeamMember()
            throws Exception {

        User admin = createUser(
                "invalid-team-admin@example.com",
                UserRole.ADMIN
        );

        User manager = createUser(
                "valid-manager@example.com",
                UserRole.MANAGER
        );

        User anotherManager = createUser(
                "target-manager@example.com",
                UserRole.MANAGER
        );

        String token = login(admin);

        mockMvc.perform(
                        put(
                                "/api/v1/admin/users/{teamMemberId}/manager",
                                anotherManager.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "managerId": "%s"
                                        }
                                        """.formatted(
                                                manager.getId()
                                        )
                                )
                )
                .andExpect(status().isConflict());
    }

    @Test
    void inactiveManagerShouldBeRejected()
            throws Exception {

        User admin = createUser(
                "inactive-manager-admin@example.com",
                UserRole.ADMIN
        );

        User manager = createUser(
                "inactive-manager@example.com",
                UserRole.MANAGER
        );

        manager.deactivate();
        userRepository.flush();

        User member = createUser(
                "inactive-manager-member@example.com",
                UserRole.TEAM_MEMBER
        );

        String token = login(admin);

        mockMvc.perform(
                        put(
                                "/api/v1/admin/users/{teamMemberId}/manager",
                                member.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "managerId": "%s"
                                        }
                                        """.formatted(
                                                manager.getId()
                                        )
                                )
                )
                .andExpect(status().isConflict());
    }

    @Test
    void adminShouldRemoveManagerAssignment()
            throws Exception {

        User admin = createUser(
                "remove-manager-admin@example.com",
                UserRole.ADMIN
        );

        User manager = createUser(
                "remove-manager@example.com",
                UserRole.MANAGER
        );

        User member = createUser(
                "remove-manager-member@example.com",
                UserRole.TEAM_MEMBER
        );

        String token = login(admin);

        assignManager(
                token,
                member.getId(),
                manager.getId()
        );

        mockMvc.perform(
                        delete(
                                "/api/v1/admin/users/{teamMemberId}/manager",
                                member.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(status().isNoContent());

        assertThat(
                managerTeamMemberRepository
                        .findByTeamMemberId(
                                member.getId()
                        )
        ).isEmpty();
    }

    @Test
    void managerShouldSeeOwnTeam()
            throws Exception {

        User admin = createUser(
                "team-admin@example.com",
                UserRole.ADMIN
        );

        User manager = createUser(
                "team-manager@example.com",
                UserRole.MANAGER
        );

        User member = createUser(
                "team-member@example.com",
                UserRole.TEAM_MEMBER
        );

        String adminToken = login(admin);

        assignManager(
                adminToken,
                member.getId(),
                manager.getId()
        );

        String managerToken = login(manager);

        mockMvc.perform(
                        get("/api/v1/manager/team-members")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(managerToken)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$[0].id")
                                .value(
                                        member.getId()
                                                .toString()
                                )
                )
                .andExpect(
                        jsonPath("$[0].email")
                                .value(
                                        member.getEmail()
                                )
                );
    }

    @Test
    void managerShouldNotAssignManagers()
            throws Exception {

        User manager = createUser(
                "unauthorized-manager@example.com",
                UserRole.MANAGER
        );

        User targetManager = createUser(
                "target-manager-two@example.com",
                UserRole.MANAGER
        );

        User member = createUser(
                "unauthorized-member@example.com",
                UserRole.TEAM_MEMBER
        );

        String token = login(manager);

        mockMvc.perform(
                        put(
                                "/api/v1/admin/users/{teamMemberId}/manager",
                                member.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "managerId": "%s"
                                        }
                                        """.formatted(
                                                targetManager.getId()
                                        )
                                )
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/v1/admin/manager-assignments")
                                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                )
                .andExpect(status().isForbidden());
    }

    private void assignManager(
            String token,
            UUID memberId,
            UUID managerId
    ) throws Exception {

        mockMvc.perform(
                        put(
                                "/api/v1/admin/users/{teamMemberId}/manager",
                                memberId
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "managerId": "%s"
                                        }
                                        """.formatted(
                                                managerId
                                        )
                                )
                )
                .andExpect(status().isNoContent());
    }

    private User createUser(
            String email,
            UserRole role
    ) {

        return userRepository.saveAndFlush(
                new User(
                        email,
                        passwordEncoder.encode(
                                "VerySecurePassword123!"
                        ),
                        "Test",
                        "User",
                        role
                )
        );
    }

    private String login(User user)
            throws Exception {

        LoginRequest request =
                new LoginRequest(
                        user.getEmail(),
                        "VerySecurePassword123!"
                );

        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/login").with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request
                                                )
                                )
                )
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                )
                .get("accessToken")
                .asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
