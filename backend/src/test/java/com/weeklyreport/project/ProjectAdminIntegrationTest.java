package com.weeklyreport.project;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import java.util.UUID;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.weeklyreport.activity.ActivityType;
import com.weeklyreport.activity.entity.ActivityLog;
import com.weeklyreport.activity.repository.ActivityLogRepository;
import com.weeklyreport.auth.dto.LoginRequest;
import com.weeklyreport.project.entity.Project;
import com.weeklyreport.project.repository.ProjectRepository;
import com.weeklyreport.report.TaskPriority;
import com.weeklyreport.report.TaskStatus;
import com.weeklyreport.report.content.CompletedTask;
import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.report.entity.WeeklyReport;
import com.weeklyreport.report.repository.CompletedTaskRepository;
import com.weeklyreport.report.repository.ReportVersionRepository;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProjectAdminIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private WeeklyReportRepository weeklyReportRepository;

    @Autowired
    private ReportVersionRepository reportVersionRepository;

    @Autowired
    private CompletedTaskRepository completedTaskRepository;

    @Test
    void adminShouldCreateProject()
            throws Exception {

        String token = createUserAndLogin(
                "admin-project@example.com",
                UserRole.ADMIN
        );

        MvcResult result =  mockMvc.perform(post("/api/v1/admin/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    {
                                        "name": "Client Portal",
                                        "description": "Customer portal"
                                    }
                                """
                        )
        ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Client Portal"))
                .andExpect(jsonPath("$.status").value("ACTIVE")).andReturn();
                

        assertThat(projectRepository.existsByNameIgnoreCaseAndStatus(
                                "Client Portal",
                                ProjectStatus.ACTIVE
                        )
        ).isTrue();

        String responseBody = result.getResponse().getContentAsString();
        String projectIdString = JsonPath.read(responseBody, "$.id");
        UUID projectId = UUID.fromString(projectIdString);

        Page<ActivityLog> activities = activityLogRepository
                        .findByActivityTypeOrderByCreatedAtDesc(
                                ActivityType.PROJECT_CREATED,
                                PageRequest.of(0, 10)
                        );

        assertThat(activities.getContent()).isNotEmpty();

        ActivityLog activity = activities.getContent().getFirst();

        assertThat(activity.getEntityId()).isEqualTo(projectId);

    }

    @Test
    void duplicateActiveProjectNameShouldReturnConflict() throws Exception {

        String token = createUserAndLogin(
                "admin-duplicate@example.com",
                UserRole.ADMIN
        );

        createProject(token, "Internal Tooling");

        mockMvc.perform(post("/api/v1/admin/projects")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "name": "INTERNAL TOOLING",
                                          "description": "Duplicate"
                                        }
                                        """
                        )
        ).andExpect(status().isConflict());
    }

    @Test
    void adminShouldUpdateProject() throws Exception {

        String token = createUserAndLogin("admin-update@example.com", UserRole.ADMIN);

        UUIDHolder project = createProject(token,"Old Name");

        mockMvc.perform(put("/api/v1/admin/projects/{id}",project.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "name": "New Name",
                                          "description": "Updated"
                                        }
                                        """
                        )
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("New Name")
                );
    }

    @Test
    void adminShouldDeleteUnusedProject() throws Exception {

        String token = createUserAndLogin(
                "admin-archive@example.com",
                UserRole.ADMIN
        );

        UUIDHolder project = createProject(token, "Archive Me" );

        mockMvc.perform(delete("/api/v1/admin/projects/{id}", project.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
        ).andExpect(status().isNoContent());

        assertThat(projectRepository.findById(project.id())).isEmpty();

        Page<ActivityLog> activities = activityLogRepository
                .findByActivityTypeOrderByCreatedAtDesc(
                        ActivityType.PROJECT_DELETED,
                        PageRequest.of(0, 10)
                );
        assertThat(activities.getContent())
                .anyMatch(activity -> activity.getEntityId().equals(project.id()));
    }

    @Test
    void adminShouldReactivateArchivedProject() throws Exception {

        String token = createUserAndLogin(
                "admin-activate@example.com",
                UserRole.ADMIN
        );

        UUIDHolder project = createProject(token, "Reactivate");

        mockMvc.perform(patch("/api/v1/admin/projects/{id}", project.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        mockMvc.perform(
                patch("/api/v1/admin/projects/{id}", project.id())
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(token)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                                .value("ACTIVE")
                );

        Project saved = projectRepository
                .findById(project.id())
                .orElseThrow();

        assertThat(saved.getStatus())
                .isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void referencedProjectShouldBeArchivedInsteadOfDeleted() throws Exception {
        String email = "admin-referenced-project@example.com";
        String token = createUserAndLogin(email, UserRole.ADMIN);
        UUIDHolder projectId = createProject(token, "Historical Project");

        User owner = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        Project project = projectRepository.findById(projectId.id()).orElseThrow();
        WeeklyReport report = weeklyReportRepository.saveAndFlush(
                new WeeklyReport(owner, LocalDate.of(2026, 8, 31))
        );
        ReportVersion version = reportVersionRepository.saveAndFlush(
                new ReportVersion(report, 1, null)
        );
        report.setCurrentVersion(version);
        weeklyReportRepository.saveAndFlush(report);
        completedTaskRepository.saveAndFlush(new CompletedTask(
                version, project, "Historical task", null,
                TaskPriority.MEDIUM, 100, 100, TaskStatus.COMPLETED,
                null, null, null, 0
        ));

        mockMvc.perform(delete("/api/v1/admin/projects/{id}", projectId.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Projects referenced by reports cannot be deleted; archive the project instead"
                ));

        assertThat(projectRepository.findById(projectId.id())).isPresent();

        mockMvc.perform(patch("/api/v1/admin/projects/{id}", projectId.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        assertThat(completedTaskRepository.existsByProjectId(projectId.id())).isTrue();
    }

    @Test
    void teamMemberShouldNotCreateProject() throws Exception {

        String token = createUserAndLogin(
                "member-create-project@example.com",
                UserRole.TEAM_MEMBER
        );

        mockMvc.perform(post("/api/v1/admin/projects")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                bearer(token)
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                        {
                                          "name": "Forbidden Project"
                                        }
                                        """
                        )
        ).andExpect(status().isForbidden());
    }

    private UUIDHolder createProject(String token, String name) throws Exception {

        MvcResult result = mockMvc.perform(
                post("/api/v1/admin/projects")
                        .header(HttpHeaders.AUTHORIZATION,
                                bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                        java.util.Map.of(
                                                "name",
                                                name
                                        )
                                )
                        )
        ).andExpect(status().isCreated()).andReturn();

        JsonNode body = objectMapper.readTree(
                result.getResponse()
                        .getContentAsString()
        );

        return new UUIDHolder(
                java.util.UUID.fromString(
                        body.get("id").asString()
                )
        );
    }

    private String createUserAndLogin(String email, UserRole role) throws Exception {

        String password = "VerySecurePassword123!";

        User user = new User(
                email,
                passwordEncoder.encode(password),
                "Test",
                "User",
                role
        );

        userRepository.saveAndFlush(user);

        LoginRequest request = new LoginRequest(email, password);

        MvcResult result = mockMvc.perform(
                post("/api/v1/auth/login").with(csrf())
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                objectMapper
                                        .writeValueAsString(
                                                request
                                        )
                        )
        ).andExpect(status().isOk()).andReturn();

        return objectMapper.readTree(
                result.getResponse()
                        .getContentAsString()
        ).get("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private record UUIDHolder(
            java.util.UUID id
            ) {

    }
}
