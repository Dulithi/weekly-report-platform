package com.weeklyreport.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class UserManagementIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private ManagerTeamMemberRepository managerTeamMemberRepository;

    @Test
    void adminShouldDeactivateUser()
            throws Exception {

        User admin = createUser(
                "user-admin@example.com",
                UserRole.ADMIN
        );

        User member = createUser(
                "deactivate-user@example.com",
                UserRole.TEAM_MEMBER
        );

        String token = login(admin);

        mockMvc.perform(
                        post(
                                "/api/v1/admin/users/{userId}/deactivate",
                                member.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(status().isNoContent());

        userRepository.flush();

        entityManager.flush();
        entityManager.clear();

        User saved = userRepository
                .findById(member.getId())
                .orElseThrow();

        assertThat(saved.isActive())
                .isFalse();
    }

    @Test
    void adminShouldActivateUserWithoutExplicitRepositorySave()
            throws Exception {

        User admin = createUser(
                "activate-admin@example.com",
                UserRole.ADMIN
        );

        User member = createUser(
                "activate-member@example.com",
                UserRole.TEAM_MEMBER
        );

        member.deactivate();
        userRepository.flush();
        entityManager.flush();
        entityManager.clear();

        String token = login(admin);

        mockMvc.perform(
                        post(
                                "/api/v1/admin/users/{userId}/activate",
                                member.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(status().isNoContent());

        userRepository.flush();

        entityManager.flush();
        entityManager.clear();

        User saved = userRepository
                .findById(member.getId())
                .orElseThrow();

        assertThat(saved.isActive())
                .isTrue();
    }

    @Test
    void adminShouldChangeUserRole()
            throws Exception {

        User admin = createUser(
                "role-admin@example.com",
                UserRole.ADMIN
        );

        User member = createUser(
                "role-member@example.com",
                UserRole.TEAM_MEMBER
        );

        String token = login(admin);

        mockMvc.perform(
                        patch(
                                "/api/v1/admin/users/{userId}/role",
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
                                          "role": "MANAGER"
                                        }
                                        """
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.role")
                                .value("MANAGER")
                );

        assertThat(
                userRepository.findById(
                                member.getId()
                        )
                        .orElseThrow()
                        .getRole()
        ).isEqualTo(UserRole.MANAGER);
    }

    @Test
    void adminShouldNotDeactivateOwnAccount()
            throws Exception {

        User admin = createUser(
                "self-admin@example.com",
                UserRole.ADMIN
        );

        String token = login(admin);

        mockMvc.perform(
                        post(
                                "/api/v1/admin/users/{userId}/deactivate",
                                admin.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(status().isConflict());
    }

    @Test
    void userListWithoutFiltersShouldNotDefaultToInactive()
            throws Exception {

        User admin = createUser(
                "list-admin@example.com",
                UserRole.ADMIN
        );

        createUser(
                "active-member@example.com",
                UserRole.TEAM_MEMBER
        );

        String token = login(admin);

        mockMvc.perform(
                        get("/api/v1/admin/users")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content")
                                .isArray()
                )
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(
                                        org.hamcrest.Matchers
                                                .greaterThanOrEqualTo(2)
                                )
                );
    }

    @Test
    void managerShouldNotChangeRoles()
            throws Exception {

        User manager = createUser(
                "role-manager@example.com",
                UserRole.MANAGER
        );

        User member = createUser(
                "role-target@example.com",
                UserRole.TEAM_MEMBER
        );

        String token = login(manager);

        mockMvc.perform(
                        patch(
                                "/api/v1/admin/users/{userId}/role",
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
                                          "role": "MANAGER"
                                        }
                                        """
                                )
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void managerWithAssignedMembersShouldNotBeDemoted()
            throws Exception {

        User admin = createUser(
                "demote-admin@example.com",
                UserRole.ADMIN
        );

        User manager = createUser(
                "demote-manager@example.com",
                UserRole.MANAGER
        );

        User member = createUser(
                "demote-member@example.com",
                UserRole.TEAM_MEMBER
        );

        managerTeamMemberRepository.saveAndFlush(
                new ManagerTeamMember(member, manager)
        );

        String token = login(admin);

        mockMvc.perform(
                        patch(
                                "/api/v1/admin/users/{userId}/role",
                                manager.getId()
                        )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                        "role": "TEAM_MEMBER"
                                        }
                                        """
                                )
                )
                .andExpect(status().isConflict());
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