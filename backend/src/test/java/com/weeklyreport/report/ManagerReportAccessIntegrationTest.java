package com.weeklyreport.report;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weeklyreport.user.UserRole;

class ManagerReportAccessIntegrationTest extends ReportIntegrationTestSupport {

    @Test
    void managerListsWholeTeamAndDraftExposesOnlyTrackingMetadata() throws Exception {
        var manager = user("report-list-manager@example.com", UserRole.MANAGER);
        var first = user("report-list-first@example.com", UserRole.TEAM_MEMBER);
        var second = user("report-list-second@example.com", UserRole.TEAM_MEMBER);
        UUID submitted = createPopulatedReport(first, LocalDate.of(2026, 8, 17), null);
        submit(first, submitted);
        UUID draft = createPopulatedReport(second, LocalDate.of(2026, 8, 24), null);

        mockMvc.perform(get("/api/v1/manager/reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager)))
                        .param("sort", "weekStart,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(submitted.toString()))
                .andExpect(jsonPath("$.content[0].member.email").value(first.getEmail()))
                .andExpect(jsonPath("$.content[0].status").value("SUBMITTED"))
                .andExpect(jsonPath("$.content[0].submittedAt").isNotEmpty())
                .andExpect(jsonPath("$.content[0].submittedVersion").doesNotExist())
                .andExpect(jsonPath("$.content[1].id").value(draft.toString()))
                .andExpect(jsonPath("$.content[1].status").value("DRAFT"))
                .andExpect(jsonPath("$.content[1].submittedAt").doesNotExist())
                .andExpect(jsonPath("$.content[1].currentVersion").doesNotExist());
    }

    @Test
    void managerCanOpenSubmittedContentButCannotOpenDraftContent() throws Exception {
        var manager = user("report-detail-manager@example.com", UserRole.MANAGER);
        var member = user("report-detail-member@example.com", UserRole.TEAM_MEMBER);
        UUID submitted = createPopulatedReport(member, LocalDate.of(2026, 8, 17), null);
        submit(member, submitted);
        UUID draft = createPopulatedReport(member, LocalDate.of(2026, 8, 24), null);
        String managerToken = login(manager);

        mockMvc.perform(get("/api/v1/manager/reports/{id}", submitted)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.id").value(member.getId().toString()))
                .andExpect(jsonPath("$.submittedVersion.versionNumber").value(1))
                .andExpect(jsonPath("$.submittedVersion.notes").value("Private until submitted"))
                .andExpect(jsonPath("$.submittedVersion.completedTasks[0].taskName")
                        .value("Completed work"));

        mockMvc.perform(get("/api/v1/manager/reports/{id}", draft)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    void managerFiltersByMemberStatusDateOverlapAndLatestSubmittedProject() throws Exception {
        var manager = user("report-filter-manager@example.com", UserRole.MANAGER);
        var first = user("report-filter-first@example.com", UserRole.TEAM_MEMBER);
        var second = user("report-filter-second@example.com", UserRole.TEAM_MEMBER);
        var alpha = project("Manager filter alpha", manager);
        var beta = project("Manager filter beta", manager);
        assignProject(alpha, first);
        assignProject(beta, first);
        assignProject(alpha, second);

        UUID matching = createPopulatedReport(first, LocalDate.of(2026, 8, 17), alpha.getId());
        submit(first, matching);
        UUID otherProject = createPopulatedReport(first, LocalDate.of(2026, 8, 24), beta.getId());
        submit(first, otherProject);
        createPopulatedReport(second, LocalDate.of(2026, 8, 17), alpha.getId()); // Draft project is private.

        mockMvc.perform(get("/api/v1/manager/reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager)))
                        .param("memberId", first.getId().toString())
                        .param("projectId", alpha.getId().toString())
                        .param("from", "2026-08-20")
                        .param("to", "2026-08-20")
                        .param("status", "SUBMITTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(matching.toString()));

        mockMvc.perform(get("/api/v1/manager/reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager)))
                        .param("projectId", alpha.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void invalidDateRangeIsRejectedAndListIsPaginated() throws Exception {
        var manager = user("report-page-manager@example.com", UserRole.MANAGER);
        var member = user("report-page-member@example.com", UserRole.TEAM_MEMBER);
        createReport(login(member), LocalDate.of(2026, 8, 17));
        createReport(login(member), LocalDate.of(2026, 8, 24));
        String token = login(manager);

        mockMvc.perform(get("/api/v1/manager/reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("from", "2026-08-25").param("to", "2026-08-17"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/manager/reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void adminCanUseManagerReportApiButMemberCannot() throws Exception {
        var admin = user("report-access-admin@example.com", UserRole.ADMIN);
        var member = user("report-access-member@example.com", UserRole.TEAM_MEMBER);

        mockMvc.perform(get("/api/v1/manager/reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(admin))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/manager/reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(member))))
                .andExpect(status().isForbidden());
    }

    private UUID createPopulatedReport(
            com.weeklyreport.user.entity.User member, LocalDate week, UUID projectId
    ) throws Exception {
        String token = login(member);
        UUID reportId = createReport(token, week);
        String project = projectId == null ? "" : "\"projectId\":\"" + projectId + "\",";
        update(token, reportId, """
                {"entityVersion":0,"notes":"Private until submitted","completedTasks":[{
                  %s"taskName":"Completed work","priority":"HIGH","plannedPercentage":100,
                  "actualPercentage":100,"status":"COMPLETED"
                }]}
                """.formatted(project)).andExpect(status().isOk());
        return reportId;
    }

    private void submit(com.weeklyreport.user.entity.User member, UUID reportId) throws Exception {
        mockMvc.perform(post("/api/v1/reports/{id}/submit", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(member))))
                .andExpect(status().isOk());
    }
}
