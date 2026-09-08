package com.weeklyreport.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.auth.dto.LoginRequest;
import com.weeklyreport.auth.dto.RegisterRequest;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginIntegrationTest
        extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void validCredentialsShouldReturnAccessTokenAndRefreshCookie()
            throws Exception {

        registerUser(
                "member@example.com",
                "VerySecurePassword123!"
        );

        LoginRequest request =
                new LoginRequest(
                        "member@example.com",
                        "VerySecurePassword123!"
                );

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
                .andExpect(
                        jsonPath("$.accessToken")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.tokenType")
                                .value("Bearer")
                )
                .andExpect(
                        jsonPath("$.expiresAt")
                                .isNotEmpty()
                )
                .andExpect(
                        header().string(
                                "Set-Cookie",
                                containsString(
                                        "weekly_report_refresh="
                                )
                        )
                )
                .andExpect(
                        header().string(
                                "Set-Cookie",
                                containsString(
                                        "HttpOnly"
                                )
                        )
                )
                .andExpect(
                        header().string(
                                "Set-Cookie",
                                containsString(
                                        "SameSite=Strict"
                                )
                        )
                );
    }

    @Test
    void wrongPasswordShouldReturnUnauthorized()
            throws Exception {

        registerUser(
                "wrong-password@example.com",
                "VerySecurePassword123!"
        );

        LoginRequest request =
                new LoginRequest(
                        "wrong-password@example.com",
                        "IncorrectPassword123!"
                );

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
                        status().isUnauthorized()
                );
    }

    @Test
    void unknownEmailShouldReturnUnauthorized()
            throws Exception {

        LoginRequest request =
                new LoginRequest(
                        "unknown@example.com",
                        "VerySecurePassword123!"
                );

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
                        status().isUnauthorized()
                );
    }

    @Test
    void inactiveUserShouldNotBeAbleToLogin()
            throws Exception {

        registerUser(
                "inactive@example.com",
                "VerySecurePassword123!"
        );

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                "inactive@example.com"
                        )
                        .orElseThrow();

        user.deactivate();

        userRepository.saveAndFlush(user);

        LoginRequest request =
                new LoginRequest(
                        "inactive@example.com",
                        "VerySecurePassword123!"
                );

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
                        status().isUnauthorized()
                );
    }

    @Test
    void loginEmailShouldBeCaseInsensitive()
            throws Exception {

        registerUser(
                "case@example.com",
                "VerySecurePassword123!"
        );

        LoginRequest request =
                new LoginRequest(
                        "CASE@EXAMPLE.COM",
                        "VerySecurePassword123!"
                );

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
                );
    }

    private void registerUser(
            String email,
            String password
    ) throws Exception {

        RegisterRequest request =
                new RegisterRequest(
                        email,
                        password,
                        "Test",
                        "User"
                );

        mockMvc.perform(
                        post("/api/v1/auth/register").with(csrf())
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
                        status().isCreated()
                );
    }
}
