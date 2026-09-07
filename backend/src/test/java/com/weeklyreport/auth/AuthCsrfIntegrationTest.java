package com.weeklyreport.auth;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;
import com.weeklyreport.support.PostgresIntegrationTest;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;

// Separate context: these tests use the actual cookie/token exchange, not the csrf() test helper.
@SpringBootTest(properties = "test.real-csrf-exchange=true")
@AutoConfigureMockMvc
class AuthCsrfIntegrationTest extends PostgresIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ObjectMapper objectMapper;

    private User user;

    @BeforeEach
    void createUser() {
        user = userRepository.saveAndFlush(new User("csrf-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("VerySecurePassword123!"), "CSRF", "Test", UserRole.TEAM_MEMBER));
    }

    @AfterEach
    void removeTestData() {
        jdbcTemplate.update("delete from refresh_token where user_id = ?", user.getId());
        userRepository.deleteById(user.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"register", "login", "refresh", "logout"})
    void authPostWithoutCsrfTokenIsRejected(String action) throws Exception {
        mockMvc.perform(post("/api/v1/auth/" + action)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void realCookieAndHeaderExchangeProtectsLoginRefreshAndLogout() throws Exception {
        CsrfProof proof = fetchCsrfProof();
        var login = mockMvc.perform(post("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .cookie(proof.cookie()).header(proof.headerName(), proof.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", user.getEmail(), "password", "VerySecurePassword123!"))))
                .andExpect(status().isOk()).andReturn();
        Cookie refresh = login.getResponse().getCookie("refresh_token");
        assertThat(refresh).isNotNull();

        // Rejection must happen before consuming or rotating the refresh token.
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh, proof.cookie()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refresh, proof.cookie())
                        .header(proof.headerName(), "invalid"))
                .andExpect(status().isForbidden());

        var renewed = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(refresh, proof.cookie()).header(proof.headerName(), proof.token()))
                .andExpect(status().isOk()).andReturn();
        Cookie replacement = renewed.getResponse().getCookie("refresh_token");
        assertThat(replacement).isNotNull();

        mockMvc.perform(post("/api/v1/auth/logout").cookie(replacement, proof.cookie()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(replacement, proof.cookie()).header(proof.headerName(), proof.token()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(replacement, proof.cookie()).header(proof.headerName(), proof.token()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void untrustedOriginIsRejectedEvenWithValidCsrfProof() throws Exception {
        CsrfProof proof = fetchCsrfProof();
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                        .cookie(proof.cookie()).header(proof.headerName(), proof.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void trustedFrontendCanPreflightTheCsrfHeader() throws Exception {
        mockMvc.perform(options("/api/v1/auth/refresh")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    private CsrfProof fetchCsrfProof() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getDomain()).isNull();
        assertThat(result.getRequest().getSession(false)).isNull();
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("token").asText()).isNotBlank();
        return new CsrfProof(cookie, body.get("headerName").asText(), body.get("token").asText());
    }

    private record CsrfProof(Cookie cookie, String headerName, String token) { }
}
