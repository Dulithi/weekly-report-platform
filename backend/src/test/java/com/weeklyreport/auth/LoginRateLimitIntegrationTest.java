package com.weeklyreport.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.auth.service.LoginRateLimitService;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@SpringBootTest(properties = "app.security.login-rate-limit.max-attempts=3")
@AutoConfigureMockMvc
class LoginRateLimitIntegrationTest extends PostgresIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private tools.jackson.databind.ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private LoginRateLimitService limiter;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private final java.util.Set<String> usedEmails = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private User user;

    @AfterEach
    void cleanupTestData() throws Exception {
        for (String email : usedEmails) {
            jdbcTemplate.update("delete from login_attempt_bucket where bucket_key = ?", key(email));
        }
        if (user != null) {
            jdbcTemplate.update("delete from refresh_token where user_id = ?", user.getId());
            userRepository.deleteById(user.getId());
        }
    }

    @Test
    void unknownEmailIsLimitedAcrossRequestsAndCaseVariants() throws Exception {
        String email = "limited-" + UUID.randomUUID() + "@example.com";
        for (int i = 0; i < 3; i++) {
            assertThat(login(i == 1 ? email.toUpperCase(java.util.Locale.ROOT) : email)
                    .getResponse().getStatus()).isEqualTo(401);
        }
        MvcResult blocked = login(email);
        assertThat(blocked.getResponse().getStatus()).isEqualTo(429);
        assertThat(Long.parseLong(blocked.getResponse().getHeader(HttpHeaders.RETRY_AFTER)))
                .isBetween(1L, 900L);
        assertThat(blocked.getResponse().getContentAsString()).doesNotContain(email);
        assertThat(login("other-" + UUID.randomUUID() + "@example.com")
                .getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void simultaneousRequestsShareOneLimit() throws Exception {
        String email = "concurrent-login-" + UUID.randomUUID() + "@example.com";
        CountDownLatch start = new CountDownLatch(1);
        List<Integer> statuses = new ArrayList<>();
        try (var executor = Executors.newFixedThreadPool(6)) {
            var futures = new ArrayList<java.util.concurrent.Future<Integer>>();
            for (int i = 0; i < 6; i++) {
                futures.add(executor.submit(() -> {
                    if (!start.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Login start timed out");
                    }
                    return login(email).getResponse().getStatus();
                }));
            }
            start.countDown();
            for (var future : futures) {
                statuses.add(future.get(10, TimeUnit.SECONDS));
            }
        }
        assertThat(statuses).containsExactlyInAnyOrder(401, 401, 401, 429, 429, 429);
    }

    @Test
    void csrfRejectionDoesNotUseTheLoginBudget() throws Exception {
        String email = "csrf-budget-" + UUID.randomUUID() + "@example.com";
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON).content(body(email)))
                    .andExpect(status().isForbidden());
        }
        assertThat(login(email).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void successfulLoginsAlsoCountAndTheAccountStaysActive() throws Exception {
        String email = "valid-limited-" + UUID.randomUUID() + "@example.com";
        user = userRepository.saveAndFlush(new User(email,
                passwordEncoder.encode("VerySecurePassword123!"), "Limited", "User", UserRole.TEAM_MEMBER));
        for (int i = 0; i < 3; i++) {
            assertThat(login(email, "VerySecurePassword123!").getResponse().getStatus()).isEqualTo(200);
        }
        assertThat(login(email, "VerySecurePassword123!").getResponse().getStatus()).isEqualTo(429);
        assertThat(userRepository.findById(user.getId()).orElseThrow().isActive()).isTrue();
    }

    @Test
    void blockedAttemptsDoNotExtendTheWindowAndExpiryRestoresTheBudget() throws Exception {
        String email = "expiry-" + UUID.randomUUID() + "@example.com";
        for (int i = 0; i < 3; i++) {
            login(email);
        }
        var deadline = jdbcTemplate.queryForObject(
                "select window_ends_at from login_attempt_bucket where bucket_key = ?",
                java.time.OffsetDateTime.class, key(email));
        assertThat(login(email).getResponse().getStatus()).isEqualTo(429);
        assertThat(jdbcTemplate.queryForObject(
                "select window_ends_at from login_attempt_bucket where bucket_key = ?",
                java.time.OffsetDateTime.class, key(email))).isEqualTo(deadline);

        expire(email);
        assertThat(login(email).getResponse().getStatus()).isEqualTo(401);
        assertThat(jdbcTemplate.queryForObject(
                "select attempts from login_attempt_bucket where bucket_key = ?",
                Integer.class, key(email))).isEqualTo(1);
    }

    @Test
    void cleanupRemovesExpiredCountersButPreservesActiveOnes() throws Exception {
        String expired = "cleanup-old-" + UUID.randomUUID() + "@example.com";
        String active = "cleanup-active-" + UUID.randomUUID() + "@example.com";
        login(expired);
        login(active);
        expire(expired);

        limiter.cleanupExpired();

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from login_attempt_bucket where bucket_key = ?",
                Integer.class, key(expired))).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "select attempts from login_attempt_bucket where bucket_key = ?",
                Integer.class, key(active))).isEqualTo(1);
    }

    private void expire(String email) throws Exception {
        // Advance stored expiry rather than waiting fifteen minutes in a test.
        jdbcTemplate.update("""
                update login_attempt_bucket set window_ends_at = statement_timestamp() - interval '1 second'
                where bucket_key = ?
                """, key(email));
    }

    private String key(String email) throws Exception {
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(email.trim().toLowerCase(java.util.Locale.ROOT)
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private MvcResult login(String email) throws Exception {
        return login(email, "IncorrectPassword123!");
    }

    private MvcResult login(String email, String password) throws Exception {
        usedEmails.add(email);
        return mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", email, "password", password))))
                .andReturn();
    }

    private String body(String email) {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "email", email, "password", "IncorrectPassword123!"));
    }
}
