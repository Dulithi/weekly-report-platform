package com.weeklyreport.support;

import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weeklyreport.auth.dto.LoginRequest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

import tools.jackson.databind.ObjectMapper;

public class AuthTestHelper {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public AuthTestHelper(UserRepository userRepository, PasswordEncoder passwordEncoder, MockMvc mockMvc,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public User createUser(
            String email,
            UserRole role
    ) {

        return userRepository.saveAndFlush(
                new User(
                        email,
                        passwordEncoder.encode(
                                "VerySecurePassword123!"
                        ),
                        "Test",
                        "User",
                        role
                )
        );
    }

    

    public String login(
            String email
    ) throws Exception {

        LoginRequest request =
                new LoginRequest(
                        email,
                        "VerySecurePassword123!"
                );

        MvcResult result =
                mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper
                                                .writeValueAsString(
                                                        request
                                                )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andReturn();

        return objectMapper
                .readTree(
                        result.getResponse()
                                .getContentAsString()
                )
                .get("accessToken")
                .asText();
    }
}