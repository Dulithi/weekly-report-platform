package com.weeklyreport.user;

import java.time.Clock;
import java.time.Instant;

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
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.support.AuthTestHelper;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.repository.UserInvitationRepository;
import com.weeklyreport.user.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserInvitationIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserInvitationRepository invitationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock clock;

    private AuthTestHelper auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestHelper(userRepository, passwordEncoder, mockMvc, objectMapper);
    }

    @Test
    void adminCreatesInvitationAndTokenCanBeAcceptedExactlyOnce() throws Exception {
        var admin = auth.createUser("invite-admin@example.com", UserRole.ADMIN);
        String adminToken = auth.login(admin.getEmail());

        MvcResult created = mockMvc.perform(post("/api/v1/admin/user-invitations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"Invited.Manager@Example.com\","
                                + "\"role\":\"MANAGER\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL,
                        org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.invitation.email")
                        .value("invited.manager@example.com"))
                .andExpect(jsonPath("$.invitation.status").value("PENDING"))
                .andExpect(jsonPath("$.acceptanceToken").isNotEmpty())
                .andReturn();
        String rawToken = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("acceptanceToken").asText();

        var stored = invitationRepository.findAll().getFirst();
        assertThat(stored.getTokenHash()).hasSize(64).doesNotContain(rawToken);

        mockMvc.perform(get("/api/v1/admin/user-invitations")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].acceptanceToken").doesNotExist());

        String acceptanceBody = """
                {
                  "acceptanceToken": "%s",
                  "password": "A secure invited password!",
                  "firstName": "Invited",
                  "lastName": "Manager"
                }
                """.formatted(rawToken);
        mockMvc.perform(post("/api/v1/auth/invitation-acceptances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptanceBody))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/auth/invitation-acceptances").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptanceBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("invited.manager@example.com"))
                .andExpect(jsonPath("$.role").value("MANAGER"));

        var invitedUser = userRepository.findByEmailIgnoreCase("invited.manager@example.com")
                .orElseThrow();
        assertThat(invitedUser.getPasswordHash()).isNotEqualTo("A secure invited password!");
        assertThat(passwordEncoder.matches(
                "A secure invited password!", invitedUser.getPasswordHash()
        )).isTrue();
        assertThat(stored.getAcceptedAt()).isNotNull();

        mockMvc.perform(post("/api/v1/auth/invitation-acceptances").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(acceptanceBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateAndExistingAccountInvitationsAreRejected() throws Exception {
        var admin = auth.createUser("invite-conflict-admin@example.com", UserRole.ADMIN);
        auth.createUser("already-registered@example.com", UserRole.TEAM_MEMBER);
        String token = auth.login(admin.getEmail());

        createInvitation(token, "pending@example.com").andExpect(status().isCreated());
        createInvitation(token, "PENDING@example.com").andExpect(status().isConflict());
        createInvitation(token, "already-registered@example.com")
                .andExpect(status().isConflict());
    }

    @Test
    void revokedInvitationCannotBeAcceptedAndMemberCannotManageInvitations() throws Exception {
        var admin = auth.createUser("invite-revoke-admin@example.com", UserRole.ADMIN);
        var member = auth.createUser("invite-revoke-member@example.com", UserRole.TEAM_MEMBER);
        String adminToken = auth.login(admin.getEmail());
        MvcResult created = createInvitation(adminToken, "revoked@example.com")
                .andExpect(status().isCreated())
                .andReturn();
        var json = objectMapper.readTree(created.getResponse().getContentAsString());
        String invitationId = json.get("invitation").get("id").asText();
        String acceptanceToken = json.get("acceptanceToken").asText();

        mockMvc.perform(delete("/api/v1/admin/user-invitations/{id}", invitationId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/invitation-acceptances").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"acceptanceToken\":\"%s\","
                                .formatted(acceptanceToken)
                                + "\"password\":\"A secure password!\","
                                + "\"firstName\":\"Test\",\"lastName\":\"User\"}"))
                .andExpect(status().isBadRequest());

        createInvitation(auth.login(member.getEmail()), "forbidden@example.com")
                .andExpect(status().isForbidden());
    }

    @Test
    void expirationIsCalculatedUsingTheConfiguredClock() throws Exception {
        var admin = auth.createUser("invite-expiry-admin@example.com", UserRole.ADMIN);
        String token = auth.login(admin.getEmail());
        MvcResult created = createInvitation(token, "expiry@example.com")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invitation.expiresAt").isNotEmpty())
                .andReturn();
        Instant expiresAt = Instant.parse(objectMapper
                .readTree(created.getResponse().getContentAsString())
                .get("invitation").get("expiresAt").asText());

        assertThat(expiresAt).isAfter(clock.instant());
        assertThat(expiresAt).isBeforeOrEqualTo(clock.instant().plusSeconds(24 * 60 * 60 + 1));
    }

    private org.springframework.test.web.servlet.ResultActions createInvitation(
            String token,
            String email
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/admin/user-invitations")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"role\":\"TEAM_MEMBER\"}"
                        .formatted(email)));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
