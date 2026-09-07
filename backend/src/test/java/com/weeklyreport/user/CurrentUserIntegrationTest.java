package com.weeklyreport.user;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.auth.dto.LoginRequest;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CurrentUserIntegrationTest
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
    void authenticatedUserShouldReturnOwnProfile()
            throws Exception {

        User user =
                createUser(
                        "current@example.com",
                        UserRole.TEAM_MEMBER
                );

        String token =
                login(
                        user.getEmail(),
                        "VerySecurePassword123!"
                );

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(token)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        user.getId().toString()
                                )
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "current@example.com"
                                )
                )
                .andExpect(
                        jsonPath("$.firstName")
                                .value(
                                        "Current"
                                )
                )
                .andExpect(
                        jsonPath("$.lastName")
                                .value(
                                        "User"
                                )
                )
                .andExpect(
                        jsonPath("$.role")
                                .value(
                                        "TEAM_MEMBER"
                                )
                );
    }

    @Test
    void anonymousUserShouldNotAccessCurrentUserEndpoint()
            throws Exception {

        mockMvc.perform(
                        get("/api/v1/users/me")
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void currentUserEndpointShouldUseJwtIdentity()
            throws Exception {

        User alice =
                createUser(
                        "alice@example.com",
                        UserRole.TEAM_MEMBER
                );

        createUser(
                "bob@example.com",
                UserRole.TEAM_MEMBER
        );

        String aliceToken =
                login(
                        alice.getEmail(),
                        "VerySecurePassword123!"
                );

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        bearer(aliceToken)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(
                                        alice.getId().toString()
                                )
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        alice.getEmail()
                                )
                );
    }

    private User createUser(
            String email,
            UserRole role
    ) {

        User user =
                new User(
                        email,
                        passwordEncoder.encode(
                                "VerySecurePassword123!"
                        ),
                        "Current",
                        "User",
                        role
                );

        return userRepository.saveAndFlush(
                user
        );
    }

    private String login(
            String email,
            String password
    ) throws Exception {

        LoginRequest request =
                new LoginRequest(
                        email,
                        password
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

        JsonNode body =
                objectMapper.readTree(
                        result.getResponse()
                                .getContentAsString()
                );

        return body
                .get("accessToken")
                .asText();
    }

    private String bearer(
            String token
    ) {
        return "Bearer " + token;
    }
}