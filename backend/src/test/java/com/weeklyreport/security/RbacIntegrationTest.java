package com.weeklyreport.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.auth.dto.LoginRequest;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
        classes = {
                com.weeklyreport.WeeklyReportApiApplication.class,
                TestProtectedController.class
        }
)
@AutoConfigureMockMvc
@Transactional
class RbacIntegrationTest
        extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void anonymousUserShouldReceiveUnauthorized()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/security-test")
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void teamMemberShouldAccessAuthenticatedRoute()
            throws Exception {

        String token =
                createUserAndLogin(
                        "member-auth@example.com",
                        UserRole.TEAM_MEMBER
                );

        mockMvc.perform(
                        get("/api/v1/security-test")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void teamMemberShouldNotAccessManagerRoute()
            throws Exception {

        String token =
                createUserAndLogin(
                        "member-manager@example.com",
                        UserRole.TEAM_MEMBER
                );

        mockMvc.perform(
                        get("/api/v1/manager/security-test")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void teamMemberShouldNotAccessAdminRoute()
            throws Exception {

        String token =
                createUserAndLogin(
                        "member-admin@example.com",
                        UserRole.TEAM_MEMBER
                );

        mockMvc.perform(
                        get("/api/v1/admin/security-test")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void managerShouldAccessManagerRoute()
            throws Exception {

        String token =
                createUserAndLogin(
                        "manager@example.com",
                        UserRole.MANAGER
                );

        mockMvc.perform(
                        get("/api/v1/manager/security-test")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void managerShouldNotAccessAdminRoute()
            throws Exception {

        String token =
                createUserAndLogin(
                        "manager-admin@example.com",
                        UserRole.MANAGER
                );

        mockMvc.perform(
                        get("/api/v1/admin/security-test")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void adminShouldAccessManagerRoute()
            throws Exception {

        String token =
                createUserAndLogin(
                        "admin-manager@example.com",
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        get("/api/v1/manager/security-test")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }

    @Test
    void adminShouldAccessAdminRoute()
            throws Exception {

        String token =
                createUserAndLogin(
                        "admin@example.com",
                        UserRole.ADMIN
                );

        mockMvc.perform(
                        get("/api/v1/admin/security-test")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }

    private String createUserAndLogin(
            String email,
            UserRole role
    ) throws Exception {

        String rawPassword =
                "VerySecurePassword123!";

        User user =
                new User(
                        email,
                        passwordEncoder.encode(
                                rawPassword
                        ),
                        "Test",
                        "User",
                        role
                );

        userRepository.saveAndFlush(
                user
        );

        LoginRequest request =
                new LoginRequest(
                        email,
                        rawPassword
                );

        MvcResult result =
                mockMvc.perform(
                                post("/api/v1/auth/login").with(csrf())
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                objectMapper.writeValueAsString(
                                                        request
                                                )
                                        )
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andReturn();

        JsonNode response =
                objectMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                );

        return response
                .get("accessToken")
                .asString();
    }

    private String bearer(
            String token
    ) {
        return "Bearer " + token;
    }
}