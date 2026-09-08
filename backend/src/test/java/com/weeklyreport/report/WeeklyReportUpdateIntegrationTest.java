package com.weeklyreport.report;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weeklyreport.user.UserRole;

class WeeklyReportUpdateIntegrationTest extends ReportIntegrationTestSupport {

    private static final String CONTENT = """
            {
              "entityVersion": 0,
              "notes": "Weekly notes",
              "completedTasks": [{
                "taskName": "Completed task",
                "description": "Done",
                "priority": "HIGH",
                "plannedPercentage": 80,
                "actualPercentage": 100,
                "status": "COMPLETED",
                "plannedMinutes": 120,
                "spentMinutes": 90,
                "deliverable": "PR-1"
              }],
              "plannedTasks": [{
                "taskName": "Planned task",
                "description": "Next",
                "priority": "MEDIUM",
                "estimatedMinutes": 60
              }],
              "blockers": [{"description": "Waiting on review", "keyBlocker": true, "resolved": false}],
              "achievements": [{"description": "Shipped feature", "keyAchievement": true}],
              "timeEntries": [{"taskType": "DEVELOPMENT", "minutes": 90}]
            }
            """;

    @Test
    void memberCanUpdateOwnDraftAndPersistsAllContent() throws Exception {
        var member = user("update-content@example.com", UserRole.TEAM_MEMBER);
        UUID reportId = createReport(login(member), LocalDate.of(2026, 8, 17));

                update(login(member), reportId, CONTENT)
                        .andExpect(status().isOk());

        mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        "/api/v1/reports/{reportId}", reportId
                ).header("Authorization", bearer(login(member)))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentVersion.notes").value("Weekly notes"))
            .andExpect(jsonPath("$.currentVersion.completedTasks[0].taskName").value("Completed task"))
            .andExpect(jsonPath("$.currentVersion.plannedTasks[0].taskName").value("Planned task"))
            .andExpect(jsonPath("$.currentVersion.blockers[0].description").value("Waiting on review"))
            .andExpect(jsonPath("$.currentVersion.achievements[0].description").value("Shipped feature"))
            .andExpect(jsonPath("$.currentVersion.timeEntries[0].minutes").value(90));
    }

    @Test
    void updateReturnsOkAndPreservesRequestOrder() throws Exception {
        var member = user("update-order@example.com", UserRole.TEAM_MEMBER);
        UUID reportId = createReport(login(member), LocalDate.of(2026, 8, 17));
        String body = """
                {"entityVersion":0,"completedTasks":[
                  {"taskName":"Second","priority":"LOW","plannedPercentage":0,"actualPercentage":20,"status":"IN_PROGRESS"},
                  {"taskName":"First","priority":"HIGH","plannedPercentage":100,"actualPercentage":100,"status":"COMPLETED"}
                ]}
                """;

        update(login(member), reportId, body);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/v1/reports/{reportId}", reportId
        ).header("Authorization", bearer(login(member))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentVersion.completedTasks[0].taskName").value("Second"))
            .andExpect(jsonPath("$.currentVersion.completedTasks[0].sortOrder").value(0))
            .andExpect(jsonPath("$.currentVersion.completedTasks[1].taskName").value("First"))
            .andExpect(jsonPath("$.currentVersion.completedTasks[1].sortOrder").value(1));
    }

    @Test
    void twoKeyBlockersAreRejected() throws Exception {
        assertUpdateRejected("update-two-blockers@example.com", """
                {"entityVersion":0,"blockers":[{"description":"A","keyBlocker":true},{"description":"B","keyBlocker":true}]}
                """);
    }

    @Test
    void twoKeyAchievementsAreRejected() throws Exception {
        assertUpdateRejected("update-two-achievements@example.com", """
                {"entityVersion":0,"achievements":[{"description":"A","keyAchievement":true},{"description":"B","keyAchievement":true}]}
                """);
    }

    @Test
    void duplicateTimeTypesAreRejected() throws Exception {
        assertUpdateRejected("update-duplicate-time@example.com", """
                {"entityVersion":0,"timeEntries":[{"taskType":"DEVELOPMENT","minutes":10},{"taskType":"DEVELOPMENT","minutes":20}]}
                """);
    }

    @Test
    void archivedProjectReferenceReturnsConflict() throws Exception {
        var member = user("update-archived@example.com", UserRole.TEAM_MEMBER);
        var project = project("Archived", member);
        project.archive();
        projectRepository.flush();
        UUID reportId = createReport(login(member), LocalDate.of(2026, 8, 17));

                update(login(member), reportId, taskWithProject(project.getId()))
                        .andExpect(status().isConflict());
    }

    @Test
    void unknownProjectReturnsNotFound() throws Exception {
        var member = user("update-unknown-project@example.com", UserRole.TEAM_MEMBER);
        UUID reportId = createReport(login(member), LocalDate.of(2026, 8, 17));

                update(login(member), reportId, taskWithProject(UUID.randomUUID()))
                        .andExpect(status().isNotFound());
    }

    @Test
    void unassignedProjectReturnsNotFound() throws Exception {
        var member = user("update-unassigned-project@example.com", UserRole.TEAM_MEMBER);
        var project = project("Not assigned", member);
        UUID reportId = createReport(login(member), LocalDate.of(2026, 8, 17));

        update(login(member), reportId, taskWithProject(project.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void putReplacesExistingContent() throws Exception {
        var member = user("update-replacement@example.com", UserRole.TEAM_MEMBER);
        String token = login(member);
        UUID reportId = createReport(token, LocalDate.of(2026, 8, 17));

        update(token, reportId, """
                {"entityVersion":0,"completedTasks":[
                  {"taskName":"Task A","priority":"LOW","plannedPercentage":0,"actualPercentage":100,"status":"COMPLETED"},
                  {"taskName":"Task B","priority":"LOW","plannedPercentage":0,"actualPercentage":100,"status":"COMPLETED"}
                ]}
                """);
        update(token, reportId, """
                {"entityVersion":0,"completedTasks":[{"taskName":"Task C","priority":"HIGH","plannedPercentage":0,"actualPercentage":100,"status":"COMPLETED"}]}
                """);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/v1/reports/{reportId}", reportId
        ).header("Authorization", bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.currentVersion.completedTasks.length()").value(1))
            .andExpect(jsonPath("$.currentVersion.completedTasks[0].taskName").value("Task C"));
    }

    private void assertUpdateRejected(String email, String body) throws Exception {
        var member = user(email, UserRole.TEAM_MEMBER);
        UUID reportId = createReport(login(member), LocalDate.of(2026, 8, 17));

        update(login(member), reportId, body)
            .andExpect(status().isBadRequest());
    }

    private String taskWithProject(UUID projectId) {
        return """
                {"entityVersion":0,"completedTasks":[{"projectId":"%s","taskName":"Task","priority":"LOW","plannedPercentage":0,"actualPercentage":100,"status":"COMPLETED"}]}
                """.formatted(projectId);
    }

    @Test
    void percentageAboveOneHundredIsRejected()
            throws Exception {

        var member =
                user(
                        "invalid-percent@example.com",
                        UserRole.TEAM_MEMBER
                );

        String token =
                login(member);

        UUID reportId =
                createReport(
                        token,
                        LocalDate.of(2026, 8, 17)
                );

        update(
                token,
                reportId,
                """
                {
                "entityVersion": 0,
                "completedTasks": [
                    {
                    "taskName": "Invalid",
                    "priority": "HIGH",
                    "plannedPercentage": 101,
                    "actualPercentage": 50,
                    "status": "IN_PROGRESS"
                    }
                ]
                }
                """
        ).andExpect(
                status().isBadRequest()
        );
    }
}
