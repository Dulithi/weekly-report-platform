package com.weeklyreport.report;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.project.entity.Project;
import com.weeklyreport.project.repository.ProjectRepository;
import com.weeklyreport.support.AuthTestHelper;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
abstract class ReportIntegrationTestSupport extends PostgresIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected AuthTestHelper auth;

    @Autowired
    protected ProjectRepository projectRepository;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;


    @Autowired
    protected  tools.jackson.databind.ObjectMapper objectMapper;

    @BeforeEach
    void setUpAuthTestHelper() {

        auth = new AuthTestHelper(
                userRepository,
                passwordEncoder,
                mockMvc,
                objectMapper
        );
    }

    protected User user(String email, UserRole role) {
        return auth.createUser(email, role);
    }

    protected Project project(String name, User creator) {
        return projectRepository.saveAndFlush(new Project(name, null, creator));
    }

    protected String login(User user) throws Exception {
        return auth.login(user.getEmail());
    }

    protected UUID createReport(String token, LocalDate weekStart) throws Exception {
        MvcResult result = mockMvc.perform(
                post("/api/v1/reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weekStart\":\"%s\"}".formatted(weekStart))
        ).andExpect(status().isCreated()).andReturn();

        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    protected ResultActions update(String token, UUID reportId, String body) throws Exception {
        return mockMvc.perform(
                put("/api/v1/reports/{reportId}", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );
    }

        protected ResultActions getReport(String token, UUID reportId) throws Exception {
        return mockMvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/v1/reports/{reportId}", reportId
            )
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        );
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}