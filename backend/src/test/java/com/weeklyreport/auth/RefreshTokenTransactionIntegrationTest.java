package com.weeklyreport.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.weeklyreport.auth.dto.LoginRequest;
import com.weeklyreport.auth.entity.RefreshToken;
import com.weeklyreport.auth.repository.RefreshTokenRepository;
import com.weeklyreport.auth.service.RefreshTokenService;
import com.weeklyreport.auth.service.AccessTokenService;
import com.weeklyreport.support.AuthTestHelper;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
// No test transaction: each HTTP request must commit or roll back independently.
class RefreshTokenTransactionIntegrationTest extends PostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @MockitoSpyBean private RefreshTokenService refreshTokenService;
    @MockitoSpyBean private AccessTokenService accessTokenService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PlatformTransactionManager transactionManager;

    private User user;

    @BeforeEach
    void createUser() {
        user = new AuthTestHelper(userRepository, passwordEncoder, mockMvc, objectMapper)
                .createUser("refresh-transaction-" + UUID.randomUUID() + "@example.com",
                        UserRole.TEAM_MEMBER);
    }

    @AfterEach
    void removeCommittedTestData() {
        if (user == null) {
            return;
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            var tokens = refreshTokenRepository.findAll().stream()
                    .filter(token -> token.getUser().getId().equals(user.getId()))
                    .toList();
            refreshTokenRepository.deleteAll(tokens);
            refreshTokenRepository.flush();
            userRepository.deleteById(user.getId());
        });
    }

    @Test
    void replayRevokesSuccessorAfterRequestCompletesButPreservesAnotherLogin() throws Exception {
        Cookie original = login();
        UUID familyId = refreshTokenService.find(original.getValue()).getFamilyId();
        Cookie separateLogin = login();
        Cookie successor = extractCookie(mockMvc.perform(
                        post("/api/v1/auth/refresh").with(csrf()).cookie(original))
                .andExpect(status().isOk()).andReturn());

        mockMvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(original))
                .andExpect(status().isUnauthorized());

        assertThat(refreshTokenRepository.findByFamilyId(familyId))
                .hasSize(2)
                .allMatch(RefreshToken::isRevoked);
        mockMvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(successor))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(separateLogin))
                .andExpect(status().isOk());
    }

    @Test
    void inactiveUserRefreshRevocationSurvivesReactivation() throws Exception {
        Cookie cookie = login();
        UUID familyId = refreshTokenService.find(cookie.getValue()).getFamilyId();
        setUserActive(false);

        mockMvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(cookie))
                .andExpect(status().isUnauthorized());

        assertThat(refreshTokenRepository.findByFamilyId(familyId))
                .hasSize(1)
                .allMatch(RefreshToken::isRevoked);
        setUserActive(true);
        mockMvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void simultaneousRefreshesCannotCreateTwoSuccessors() throws Exception {
        Cookie original = login();
        UUID familyId = refreshTokenService.find(original.getValue()).getFamilyId();

        assertOverlappingRefreshes(original, original);

        assertThat(refreshTokenRepository.findByFamilyId(familyId))
                .hasSize(2)
                .allMatch(RefreshToken::isRevoked);
    }

    @Test
    void replayDuringSuccessorRefreshCannotLeaveAnActiveDescendant() throws Exception {
        Cookie original = login();
        UUID familyId = refreshTokenService.find(original.getValue()).getFamilyId();
        Cookie successor = extractCookie(mockMvc.perform(
                        post("/api/v1/auth/refresh").with(csrf()).cookie(original))
                .andExpect(status().isOk()).andReturn());

        assertOverlappingRefreshes(successor, original);

        assertThat(refreshTokenRepository.findByFamilyId(familyId))
                .hasSize(3)
                .allMatch(RefreshToken::isRevoked);
    }

    private void assertOverlappingRefreshes(Cookie first, Cookie second) throws Exception {
        assertOverlappingRequests(first, second, "/api/v1/auth/refresh", 401);
    }

    @Test
    void logoutDuringRefreshRevokesTheReplacement() throws Exception {
        Cookie original = login();
        UUID familyId = refreshTokenService.find(original.getValue()).getFamilyId();

        assertOverlappingRequests(original, original, "/api/v1/auth/logout", 204);

        assertThat(refreshTokenRepository.findByFamilyId(familyId))
                .hasSize(2)
                .allMatch(RefreshToken::isRevoked);
    }

    private void assertOverlappingRequests(
            Cookie first, Cookie second, String secondPath, int expectedSecondStatus
    ) throws Exception {
        CountDownLatch firstIssuing = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);
        AtomicInteger consumes = new AtomicInteger();
        AtomicInteger issuances = new AtomicInteger();

        doAnswer(invocation -> {
            if (consumes.incrementAndGet() == 2) {
                secondEntered.countDown();
            }
            return invocation.callRealMethod();
        }).when(refreshTokenService).consumeForRotation(anyString());
        doAnswer(invocation -> {
            secondEntered.countDown();
            return invocation.callRealMethod();
        }).when(refreshTokenService).revokeIfPresent(anyString());
        doAnswer(invocation -> {
            if (issuances.incrementAndGet() == 1) {
                firstIssuing.countDown();
                if (!releaseFirst.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to release first refresh");
                }
            }
            return invocation.callRealMethod();
        }).when(accessTokenService).create(any(User.class));

        try (var executor = Executors.newFixedThreadPool(2)) {
            try {
                var firstRequest = executor.submit(() -> mockMvc.perform(
                        post("/api/v1/auth/refresh").with(csrf()).cookie(first)).andReturn());
                assertThat(firstIssuing.await(10, TimeUnit.SECONDS)).isTrue();
                var secondRequest = executor.submit(() -> mockMvc.perform(
                        post(secondPath).with(csrf()).cookie(second)).andReturn());
                assertThat(secondEntered.await(10, TimeUnit.SECONDS)).isTrue();

                // Give the second transaction time to reach the database while the first is paused.
                // With locking it waits; without locking it can already issue a second token.
                try {
                    secondRequest.get(1, TimeUnit.SECONDS);
                } catch (TimeoutException expectedWhileLocked) {
                    // Release the first request below so both transactions can finish.
                }
                releaseFirst.countDown();

                assertThat(firstRequest.get(10, TimeUnit.SECONDS).getResponse().getStatus())
                        .isEqualTo(200);
                assertThat(secondRequest.get(10, TimeUnit.SECONDS).getResponse().getStatus())
                        .isEqualTo(expectedSecondStatus);
            } finally {
                releaseFirst.countDown();
            }
        }
    }

    private void setUserActive(boolean active) {
        new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            User persisted = userRepository.findById(user.getId()).orElseThrow();
            if (active) {
                persisted.activate();
            } else {
                persisted.deactivate();
            }
        });
    }

    private Cookie login() throws Exception {
        return extractCookie(mockMvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(user.getEmail(), "VerySecurePassword123!"))))
                .andExpect(status().isOk()).andReturn());
    }

    private Cookie extractCookie(MvcResult result) {
        String header = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(header).isNotNull();
        String[] parts = header.split(";", 2)[0].split("=", 2);
        return new Cookie(parts[0], parts[1]);
    }
}
