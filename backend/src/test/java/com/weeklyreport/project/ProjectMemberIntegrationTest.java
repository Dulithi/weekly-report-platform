package com.weeklyreport.project;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.auth.dto.LoginRequest;
import com.weeklyreport.project.entity.Project;
import com.weeklyreport.project.repository.ProjectMemberRepository;
import com.weeklyreport.project.repository.ProjectRepository;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectMemberIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminShouldAssignProjectMember() throws Exception {

        User admin = createUser(
                "pm-admin@example.com",
                UserRole.ADMIN
        );

        User member = createUser(
                "pm-member@example.com",
                UserRole.TEAM_MEMBER
        );

        Project project = createProject(
                "Project Membership",
                admin
        );

        String token = login(admin);

        mockMvc.perform(
                post(
                        "/api/v1/admin/projects/{projectId}/members",
                        project.getId()
                ).header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(token)
                        ).contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "userId": "%s"
                                        }
                                        """.formatted(
                                        member.getId()
                                )
                        )
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                                .value(member.getId().toString())
                );

        assertThat(projectMemberRepository
                        .existsByProjectIdAndUserId(
                                project.getId(),
                                member.getId()
                        )
        ).isTrue();
    }

    @Test
    void duplicateAssignmentShouldReturnConflict()throws Exception {

        User admin = createUser(
                "pm-duplicate-admin@example.com",
                UserRole.ADMIN
        );

        User member = createUser(
                "pm-duplicate-member@example.com",
                UserRole.TEAM_MEMBER
        );

        Project project = createProject(
                "Duplicate Membership",
                admin
        );

        String token = login(admin);

        assign(token, project.getId(), member.getId());

        mockMvc.perform(
                post(
                        "/api/v1/admin/projects/{projectId}/members",
                        project.getId()
                )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(token)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "userId": "%s"
                                        }
                                        """.formatted(
                                        member.getId()
                                )
                        )
        )
                .andExpect(status().isConflict());
    }

    @Test
    void inactiveUserShouldNotBeAssigned() throws Exception {

        User admin = createUser(
                "pm-inactive-admin@example.com",
                UserRole.ADMIN
        );

        User member = createUser(
                "pm-inactive-member@example.com",
                UserRole.TEAM_MEMBER
        );

        member.deactivate();
        userRepository.flush();

        Project project = createProject(
                "Inactive Membership",
                admin
        );

        String token = login(admin);

        mockMvc.perform(
                post(
                        "/api/v1/admin/projects/{projectId}/members",
                        project.getId()
                )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(token)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "userId": "%s"
                                        }
                                        """.formatted(
                                        member.getId()
                                )
                        )
        )
                .andExpect(status().isConflict());
    }

    @Test
    void archivedProjectShouldRejectNewMembers() throws Exception {

        User admin = createUser(
                "pm-archived-admin@example.com",
                UserRole.ADMIN
        );

        User member = createUser(
                "pm-archived-member@example.com",
                UserRole.TEAM_MEMBER
        );

        Project project = createProject(
                "Archived Membership",
                admin
        );

        project.archive();
        projectRepository.flush();

        String token = login(admin);

        mockMvc.perform(
                post(
                        "/api/v1/admin/projects/{projectId}/members",
                        project.getId()
                )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(token)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "userId": "%s"
                                        }
                                        """.formatted(
                                        member.getId()
                                )
                        )
        )
                .andExpect(status().isConflict());
    }

    @Test
    void adminShouldRemoveProjectMember() throws Exception {

        User admin = createUser(
                "pm-remove-admin@example.com",
                UserRole.ADMIN
        );

        User member = createUser(
                "pm-remove-member@example.com",
                UserRole.TEAM_MEMBER
        );

        Project project = createProject(
                "Remove Membership",
                admin
        );

        String token = login(admin);

        assign(
                token,
                project.getId(),
                member.getId()
        );

        mockMvc.perform(
                delete(
                        "/api/v1/admin/projects/{projectId}/members/{memberId}",
                        project.getId(),
                        member.getId()
                )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(token)
                        )
        )
                .andExpect(status().isNoContent());

        assertThat(
                projectMemberRepository
                        .existsByProjectIdAndUserId(
                                project.getId(),
                                member.getId()
                        )
        ).isFalse();
    }

    @Test
    void managerShouldNotAssignProjectMembers() throws Exception {

        User admin = createUser(
                "pm-owner@example.com",
                UserRole.ADMIN
        );

        User manager = createUser(
                "pm-manager@example.com",
                UserRole.MANAGER
        );

        User member = createUser(
                "pm-member-forbidden@example.com",
                UserRole.TEAM_MEMBER
        );

        Project project = createProject(
                "Protected Membership",
                admin
        );

        String token = login(manager);

        mockMvc.perform(
                post(
                        "/api/v1/admin/projects/{projectId}/members",
                        project.getId()
                )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(token)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "userId": "%s"
                                        }
                                        """.formatted(
                                        member.getId()
                                )
                        )
        )
                .andExpect(status().isForbidden());
    }

    private void assign(String token, UUID projectId, UUID userId) throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/admin/projects/{projectId}/members",
                        projectId
                ).header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(token)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "userId": "%s"
                                        }
                                        """.formatted(userId)
                        )
        )
                .andExpect(status().isOk());
    }

    private User createUser(String email, UserRole role) {

        User user = new User(
                email,
                passwordEncoder.encode(
                        "VerySecurePassword123!"
                ),
                "Test",
                "User",
                role
        );

        return userRepository.saveAndFlush(user);
    }

    private Project createProject( String name, User creator) {

        return projectRepository.saveAndFlush(
                new Project(
                        name,
                        null,
                        creator
                )
        );
    }

    private String login(User user) throws Exception {

        LoginRequest request
                = new LoginRequest(
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
