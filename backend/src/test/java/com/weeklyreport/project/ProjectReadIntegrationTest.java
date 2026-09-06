package com.weeklyreport.project;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.auth.dto.LoginRequest;
import com.weeklyreport.project.entity.Project;
import com.weeklyreport.project.repository.ProjectRepository;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectReadIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void authenticatedMemberShouldListProjects() throws Exception {

        User member = createUser(
                "project-reader@example.com",
                UserRole.TEAM_MEMBER
        );

        createProject(
                "Visible Project",
                member
        );

        String token = login(member);

        mockMvc.perform(
                get("/api/v1/projects")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(token)
                        )
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content")
                                .isArray()
                );
    }

    @Test
    void statusFilterShouldReturnOnlyActiveProjects() throws Exception {

        User user = createUser(
                "filter-project@example.com",
                UserRole.ADMIN
        );

        createProject(
                "Active Project",
                user
        );

        Project archived = createProject(
                "Archived Project",
                user
        );

        archived.archive();
        projectRepository.flush();

        String token = login(user);

        mockMvc.perform(
                get("/api/v1/projects")
                        .param(
                                "status",
                                "ACTIVE"
                        )
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(token)
                        )
        )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content[0].status")
                                .value("ACTIVE")
                );
    }

    @Test
    void anonymousUserShouldNotReadProjects() throws Exception {

        mockMvc.perform(
                get("/api/v1/projects")
        )
                .andExpect(status().isUnauthorized());
    }

    private User createUser(String email, UserRole role) {

        return userRepository.saveAndFlush(
                new User(
                        email,
                        passwordEncoder.encode(
                                "VerySecurePassword123!"
                        ),
                        "Project",
                        "Reader",
                        role
                )
        );
    }

    private Project createProject(String name, User creator) {

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
                post("/api/v1/auth/login")
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
