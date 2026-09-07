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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class RefreshTokenIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void validRefreshTokenShouldReturnNewAccessToken()
            throws Exception {

        Cookie refreshCookie =
                registerLoginAndGetRefreshCookie(
                        "refresh@example.com"
                );

        mockMvc.perform(
                        post("/api/v1/auth/refresh").with(csrf())
                                .cookie(refreshCookie)
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
                        header().string(
                                HttpHeaders.SET_COOKIE,
                                containsString(
                                        "refresh_token="
                                )
                        )
                );
    }

    @Test
    void refreshShouldRotateRefreshToken()
            throws Exception {

        Cookie firstCookie =
                registerLoginAndGetRefreshCookie(
                        "rotation@example.com"
                );

        MvcResult refreshResult =
                mockMvc.perform(
                                post("/api/v1/auth/refresh").with(csrf())
                                        .cookie(firstCookie)
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andReturn();

        Cookie secondCookie = extractRefreshCookie(refreshResult);


        assertThat(
                secondCookie.getValue()
        ).isNotEqualTo(
                firstCookie.getValue()
        );

        List<RefreshToken> tokens =
                refreshTokenRepository.findAll();

        assertThat(tokens)
                .hasSize(2);

        long revokedCount =
                tokens.stream()
                        .filter(
                                RefreshToken::isRevoked
                        )
                        .count();

        long activeCount =
                tokens.stream()
                        .filter(
                                token -> !token.isRevoked()
                        )
                        .count();

        assertThat(revokedCount)
                .isEqualTo(1);

        assertThat(activeCount)
                .isEqualTo(1);

        assertThat(
                tokens.get(0).getFamilyId()
        ).isEqualTo(
                tokens.get(1).getFamilyId()
        );
    }

    @Test
    void rotatedRefreshTokenShouldNotBeReusable()
            throws Exception {

        Cookie firstCookie =
                registerLoginAndGetRefreshCookie(
                        "reuse@example.com"
                );

        mockMvc.perform(
                        post("/api/v1/auth/refresh").with(csrf())
                                .cookie(firstCookie)
                )
                .andExpect(
                        status().isOk()
                );

        mockMvc.perform(
                        post("/api/v1/auth/refresh").with(csrf())
                                .cookie(firstCookie)
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void requestWithoutRefreshCookieShouldReturnUnauthorized()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/refresh").with(csrf())
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }

    @Test
    void invalidRefreshTokenShouldReturnUnauthorized()
            throws Exception {

        mockMvc.perform(
                        post("/api/v1/auth/refresh").with(csrf())
                                .cookie(
                                    new Cookie(
                                        "refresh-token", 
                                        "invalid-token"
                                    )
                                )
                )
                .andExpect(
                        status().isUnauthorized()
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