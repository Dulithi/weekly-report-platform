package com.weeklyreport.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.auth.dto.RegisterRequest;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistrationIntegrationTest
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
    void registrationCreatesTeamMemberWithHashedPassword()
            throws Exception {

        RegisterRequest request
                = new RegisterRequest(
                        "DULITHI@example.com",
                        "verySecurePassword123!",
                        "Dulithi",
                        "Jayasooriya"
                );

        mockMvc.perform(
                post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role")
                        .value("TEAM_MEMBER")
                )
                .andExpect(
                        jsonPath("$.email")
                                .value("dulithi@example.com")
                );

        var user = userRepository
                .findByEmailIgnoreCase("dulithi@example.com")
                .orElseThrow();

        assertThat(user.getRole()).isEqualTo(UserRole.TEAM_MEMBER);

        assertThat(user.getPasswordHash())
                .isNotEqualTo(request.password());

        assertThat(
                passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash()
                )
        ).isTrue();
    }

    @Test
    void duplicateEmailReturnsConflict()
            throws Exception {

        RegisterRequest request
                = new RegisterRequest(
                        "duplicate@example.com",
                        "verySecurePassword123!",
                        "First",
                        "User"
                );

        register(request);

        mockMvc.perform(
                post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            objectMapper.writeValueAsString(request)
                        )
        ).andExpect( status().isConflict());
    }

    @Test
    void shortPasswordReturnsBadRequest() throws Exception {

        RegisterRequest request = new RegisterRequest(
                        "short@example.com",
                        "short",
                        "Test",
                        "User"
                );

        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest());
    }

    private void register(RegisterRequest request) throws Exception {

        mockMvc.perform(
                post("/api/v1/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        );
    }
}
