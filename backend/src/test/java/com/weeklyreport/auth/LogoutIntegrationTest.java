package com.weeklyreport.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.auth.dto.LoginRequest;
import com.weeklyreport.auth.dto.RegisterRequest;
import com.weeklyreport.auth.entity.RefreshToken;
import com.weeklyreport.auth.repository.RefreshTokenRepository;
import com.weeklyreport.support.PostgresIntegrationTest;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LogoutIntegrationTest
        extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void logoutShouldRevokeRefreshTokenAndClearCookie()
            throws Exception {

        Cookie refreshCookie =
                registerLoginAndGetRefreshCookie(
                        "logout@example.com"
                );

        List<RefreshToken> beforeLogout =
                refreshTokenRepository.findAll();

        assertThat(beforeLogout)
                .hasSize(1);

        assertThat(
                beforeLogout.getFirst().isRevoked()
        ).isFalse();

        mockMvc.perform(
                        post("/api/v1/auth/logout").with(csrf())
                                .cookie(refreshCookie)
                )
                .andExpect(
                        status().isNoContent()
                )
                .andExpect(
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString(
                                        "Max-Age=0"
                                )
                        )
                );

        RefreshToken token =
                refreshTokenRepository
                        .findAll()
                        .getFirst();

        assertThat(
                token.isRevoked()
        ).isTrue();
    }

    @Test
    void refreshTokenShouldNotWorkAfterLogout()
            throws Exception {

        Cookie refreshCookie =
                registerLoginAndGetRefreshCookie(
                        "logout-refresh@example.com"
                );

        mockMvc.perform(
                        post("/api/v1/auth/logout").with(csrf())
                                .cookie(refreshCookie)
                )
                .andExpect(
                        status().isNoContent()
                );

        mockMvc.perform(
                        post("/api/v1/auth/refresh").with(csrf())
                                .header(
                                        HttpHeaders.COOKIE,
                                        refreshCookie
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void logoutWithoutCookieShouldStillBeIdempotent()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/logout").with(csrf())
                )
                .andExpect(
                        status().isNoContent()
                );
    }

    @Test
    void logoutWithInvalidCookieShouldStillBeIdempotent()
            throws Exception {

        Cookie cookie =
                new Cookie(
                        "refresh_token",
                        "not-a-real-token"
                );

        mockMvc.perform(
                        post("/api/v1/auth/logout").with(csrf())
                                .cookie(cookie)
                )
                .andExpect(
                        status().isNoContent()
                );
    }

    private Cookie registerLoginAndGetRefreshCookie(
            String email
    ) throws Exception {

        RegisterRequest registerRequest =
                new RegisterRequest(
                        email,
                        "VerySecurePassword123!",
                        "Refresh",
                        "User"
                );

        mockMvc.perform(
                        post("/api/v1/auth/register").with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        registerRequest
                                                )
                                )
                )
                .andExpect(
                        status().isCreated()
                );

        LoginRequest loginRequest =
                new LoginRequest(
                        email,
                        "VerySecurePassword123!"
                );

        MvcResult loginResult =
                mockMvc.perform(
                                post("/api/v1/auth/login").with(csrf())
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                objectMapper
                                                        .writeValueAsString(
                                                                loginRequest
                                                        )
                                        )
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andReturn();

        return extractRefreshCookie(
                loginResult
        );
    }

    private Cookie extractRefreshCookie(
            MvcResult result
    ) {

        String setCookie =
                result.getResponse()
                        .getHeader(
                                HttpHeaders.SET_COOKIE
                        );

        assertThat(setCookie)
                .isNotNull();

        String cookieValue =
                setCookie.split(";", 2)[0];

        String[] parts =
                cookieValue.split("=", 2);

        return new Cookie(
                parts[0],
                parts[1]
        );
    }
}