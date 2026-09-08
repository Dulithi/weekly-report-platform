package com.weeklyreport.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.weeklyreport.support.AuthTestHelper;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
// Requests use real, separate transactions so committed admin changes are tested.
class CurrentAccountAuthorizationIntegrationTest extends PostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private tools.jackson.databind.ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtEncoder jwtEncoder;
    @Autowired private SecurityProperties securityProperties;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    private final List<UUID> createdUserIds = new ArrayList<>();
    private AuthTestHelper auth;

    @BeforeEach
    void configureHelper() {
        auth = new AuthTestHelper(userRepository, passwordEncoder, mockMvc, objectMapper);
    }

    @AfterEach
    void removeCommittedTestData() {
        new TransactionTemplate(transactionManager).executeWithoutResult(transaction -> {
            for (UUID id : createdUserIds) {
                jdbcTemplate.update("delete from activity_log where actor_user_id = ?", id);
                jdbcTemplate.update("delete from refresh_token where user_id = ?", id);
            }
            for (UUID id : createdUserIds) {
                jdbcTemplate.update("delete from app_user where id = ?", id);
            }
        });
    }

    @Test
    void deactivationRejectsAnAlreadyIssuedAccessToken() throws Exception {
        User admin = createUser(UserRole.ADMIN);
        User member = createUser(UserRole.TEAM_MEMBER);
        String adminToken = auth.login(admin.getEmail());
        String memberToken = auth.login(member.getEmail());
        read("/api/v1/users/me", memberToken).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/users/{id}", member.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isNoContent());

        read("/api/v1/users/me", memberToken).andExpect(status().isUnauthorized());
        read("/api/v1/projects", memberToken).andExpect(status().isUnauthorized());
        read("/api/v1/admin/users", adminToken).andExpect(status().isOk());
    }

    @Test
    void demotionRemovesOldAdminPermissionsWithoutRefreshingTheToken() throws Exception {
        User admin = createUser(UserRole.ADMIN);
        User target = createUser(UserRole.ADMIN);
        String adminToken = auth.login(admin.getEmail());
        String targetToken = auth.login(target.getEmail());
        read("/api/v1/admin/users", targetToken).andExpect(status().isOk());

        changeRole(adminToken, target, UserRole.TEAM_MEMBER);

        read("/api/v1/admin/users", targetToken).andExpect(status().isForbidden());
        read("/api/v1/manager/team-members", targetToken).andExpect(status().isForbidden());
        read("/api/v1/users/me", targetToken).andExpect(status().isOk());
        read("/api/v1/reports/me", targetToken).andExpect(status().isOk());
    }

    @Test
    void promotionUsesTheCurrentRoleWithoutRefreshingTheToken() throws Exception {
        User admin = createUser(UserRole.ADMIN);
        User target = createUser(UserRole.TEAM_MEMBER);
        String adminToken = auth.login(admin.getEmail());
        String targetToken = auth.login(target.getEmail());
        read("/api/v1/manager/team-members", targetToken).andExpect(status().isForbidden());

        changeRole(adminToken, target, UserRole.MANAGER);

        read("/api/v1/manager/team-members", targetToken).andExpect(status().isOk());
        read("/api/v1/admin/users", targetToken).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"not-a-uuid", "00000000-0000-0000-0000-000000000001"})
    void missingMalformedOrUnknownAccountSubjectIsRejected(String subject) throws Exception {
        read("/api/v1/projects", signedToken(subject, Instant.now().plusSeconds(300)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenIsRejectedEvenWhenTheAccountIsActive() throws Exception {
        User member = createUser(UserRole.TEAM_MEMBER);
        read("/api/v1/projects", signedToken(member.getId().toString(),
                        Instant.now().minusSeconds(300)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidSignatureIsRejectedEvenWhenTheAccountIsActive() throws Exception {
        User member = createUser(UserRole.TEAM_MEMBER);
        String token = auth.login(member.getEmail());
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String tampered = token.substring(0, signatureStart) + replacement
                + token.substring(signatureStart + 1);

        read("/api/v1/projects", tampered).andExpect(status().isUnauthorized());
    }

    private User createUser(UserRole role) {
        User user = auth.createUser("current-account-" + UUID.randomUUID() + "@example.com", role);
        createdUserIds.add(user.getId());
        return user;
    }

    private ResultActions read(String path, String token) throws Exception {
        return mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private void changeRole(String adminToken, User target, UserRole role) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/{id}/role", target.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"" + role.name() + "\"}"))
                .andExpect(status().isOk());
    }

    private String signedToken(String subject, Instant expiresAt) {
        var claims = JwtClaimsSet.builder()
                .issuer(securityProperties.jwt().issuer())
                .issuedAt(Instant.now().minusSeconds(600))
                .expiresAt(expiresAt)
                .claim("roles", List.of("ADMIN"));
        if (subject != null) {
            claims.subject(subject);
        }
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims.build()))
                .getTokenValue();
    }
}
