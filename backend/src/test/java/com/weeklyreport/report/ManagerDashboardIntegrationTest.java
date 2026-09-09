package com.weeklyreport.report;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.weeklyreport.user.UserRole;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ManagerDashboardIntegrationTest extends ReportIntegrationTestSupport {

    @Test
    void dashboardAggregatesRosterSubmittedContentTrendsAndRecentActivity() throws Exception {
        var manager = user("dashboard-manager@example.com", UserRole.MANAGER);
        var submittedMember = user("dashboard-submitted@example.com", UserRole.TEAM_MEMBER);
        var draftMember = user("dashboard-draft@example.com", UserRole.TEAM_MEMBER);
        assignManager(manager, submittedMember);
        assignManager(manager, draftMember);
        var missingMember = user("dashboard-missing@example.com", UserRole.TEAM_MEMBER);
        var inactiveMember = user("dashboard-inactive@example.com", UserRole.TEAM_MEMBER);
        var unassignedMember = user("dashboard-unassigned@example.com", UserRole.TEAM_MEMBER);
        assignManager(manager, missingMember);
        assignManager(manager, inactiveMember);
        var project = project("Atlas", manager);
        assignProject(project, submittedMember);
        LocalDate week = LocalDate.of(2099, 1, 5);

        String submittedToken = login(submittedMember);
        UUID reportId = createReport(submittedToken, week);
        update(submittedToken, reportId, """
                {
                  "entityVersion": 0,
                  "completedTasks": [
                    {
                      "projectId": "%s",
                      "taskName": "Released dashboard API",
                      "priority": "HIGH",
                      "plannedPercentage": 100,
                      "actualPercentage": 100,
                      "status": "COMPLETED"
                    },
                    {
                      "taskName": "Follow-up work",
                      "priority": "MEDIUM",
                      "plannedPercentage": 50,
                      "actualPercentage": 25,
                      "status": "IN_PROGRESS"
                    }
                  ],
                  "blockers": [
                    {"description": "Waiting for access", "keyBlocker": true, "resolved": false},
                    {"description": "Old issue", "keyBlocker": false, "resolved": true}
                  ],
                  "timeEntries": [
                    {"taskType": "DEVELOPMENT", "minutes": 120},
                    {"taskType": "TESTING", "minutes": 30}
                  ]
                }
                """.formatted(project.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reports/{id}/submit", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(submittedToken)))
                .andExpect(status().isOk());

        String managerToken = login(manager);
        mockMvc.perform(post("/api/v1/manager/reports/{id}/reviews", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"CHANGES_REQUESTED","comment":"Add evidence"}
                                """))
                .andExpect(status().isOk());
        createReport(login(draftMember), week);

        String inactiveToken = login(inactiveMember);
        UUID inactiveReportId = createReport(inactiveToken, week);
        update(inactiveToken, inactiveReportId, """
                {
                  "entityVersion": 0,
                  "completedTasks": [{
                    "taskName": "Former member work",
                    "priority": "LOW",
                    "plannedPercentage": 100,
                    "actualPercentage": 100,
                    "status": "COMPLETED"
                  }],
                  "blockers": [{
                    "description": "Former member blocker",
                    "keyBlocker": true,
                    "resolved": false
                  }],
                  "timeEntries": [{"taskType": "DEVELOPMENT", "minutes": 999}]
                }
                """).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reports/{id}/submit", inactiveReportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(inactiveToken)))
                .andExpect(status().isOk());
        inactiveMember.deactivate();
        userRepository.saveAndFlush(inactiveMember);

        String unassignedToken = login(unassignedMember);
        UUID unassignedReportId = createReport(unassignedToken, week);
        update(unassignedToken, unassignedReportId, """
                {"entityVersion":0,
                 "blockers":[{"description":"Unassigned private blocker","keyBlocker":true,"resolved":false}],
                 "timeEntries":[{"taskType":"DEVELOPMENT","minutes":777}]}
                """).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reports/{id}/submit", unassignedReportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(unassignedToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/manager/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .param("weekStart", week.toString())
                        .param("trendWeeks", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value(week.toString()))
                .andExpect(jsonPath("$.weekEnd").value(week.plusDays(6).toString()))
                .andExpect(jsonPath("$.dueAt").isNotEmpty())
                .andExpect(jsonPath("$.summary.totalActiveMembers").value(3))
                .andExpect(jsonPath("$.summary.submittedReports").value(1))
                .andExpect(jsonPath("$.summary.onTimeSubmissions").value(1))
                .andExpect(jsonPath("$.summary.pendingSubmissions").value(2))
                .andExpect(jsonPath("$.summary.lateSubmissions").value(0))
                .andExpect(jsonPath("$.summary.submissionRatePercent").value(33.3))
                .andExpect(jsonPath("$.summary.onTimeComplianceRatePercent").value(33.3))
                .andExpect(jsonPath("$.summary.needsCorrectionReports").value(1))
                .andExpect(jsonPath("$.summary.openBlockers").value(1))
                .andExpect(jsonPath("$.submissionsByMember.length()").value(3))
                .andExpect(jsonPath("$.submissionsByMember[?(@.member.id == '%s')].status"
                        .formatted(submittedMember.getId())).value(hasItem("NEEDS_CORRECTION")))
                .andExpect(jsonPath("$.submissionsByMember[?(@.member.id == '%s')].status"
                        .formatted(draftMember.getId())).value(hasItem("DRAFT")))
                .andExpect(jsonPath("$.submissionsByMember[?(@.member.id == '%s')].status"
                        .formatted(missingMember.getId())).value(hasItem("NOT_STARTED")))
                .andExpect(jsonPath("$.completedTaskTrend.length()").value(3))
                .andExpect(jsonPath("$.completedTaskTrend[0].completedTasks").value(0))
                .andExpect(jsonPath("$.completedTaskTrend[1].completedTasks").value(0))
                .andExpect(jsonPath("$.completedTaskTrend[2].weekStart").value(week.toString()))
                .andExpect(jsonPath("$.completedTaskTrend[2].completedTasks").value(1))
                .andExpect(jsonPath("$.projectTaskDistribution.length()").value(2))
                .andExpect(jsonPath("$.projectTaskDistribution[?(@.projectName == 'Atlas')].taskCount")
                        .value(hasItem(1)))
                .andExpect(jsonPath("$.projectTaskDistribution[?(@.projectName == 'Uncategorized')].taskCount")
                        .value(hasItem(1)))
                .andExpect(jsonPath("$.timeByTaskType[0].taskType").value("DEVELOPMENT"))
                .andExpect(jsonPath("$.timeByTaskType[0].minutes").value(120))
                .andExpect(jsonPath("$.timeByTaskType[1].taskType").value("TESTING"))
                .andExpect(jsonPath("$.timeByTaskType[1].minutes").value(30))
                .andExpect(jsonPath("$.recentActivity[*].type")
                        .value(hasItem("REPORT_CHANGES_REQUESTED")))
                .andExpect(jsonPath("$.recentActivity[*].reportId")
                        .value(hasItem(reportId.toString())))
                .andExpect(jsonPath("$.recentActivity[*].reportId")
                        .value(org.hamcrest.Matchers.not(hasItem(unassignedReportId.toString()))))
                .andExpect(jsonPath("$.recentActivity[*].actor.id")
                        .value(hasItem(manager.getId().toString())));
    }

    @Test
    void dashboardCountsBothLateSubmissionAndOverdueMissingReportAsLate() throws Exception {
        var manager = user("dashboard-late-manager@example.com", UserRole.MANAGER);
        var lateMember = user("dashboard-late-member@example.com", UserRole.TEAM_MEMBER);
        var overdueMember = user("dashboard-overdue-member@example.com", UserRole.TEAM_MEMBER);
        assignManager(manager, lateMember);
        assignManager(manager, overdueMember);
        LocalDate pastWeek = LocalDate.of(2026, 8, 17);

        String memberToken = login(lateMember);
        UUID reportId = createReport(memberToken, pastWeek);
        update(memberToken, reportId,
                "{\"entityVersion\":0,\"notes\":\"Late weekly update\"}")
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reports/{id}/submit", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(memberToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/manager/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager)))
                        .param("weekStart", pastWeek.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalActiveMembers").value(2))
                .andExpect(jsonPath("$.summary.submittedReports").value(1))
                .andExpect(jsonPath("$.summary.onTimeSubmissions").value(0))
                .andExpect(jsonPath("$.summary.pendingSubmissions").value(0))
                .andExpect(jsonPath("$.summary.lateSubmissions").value(2))
                .andExpect(jsonPath("$.summary.submissionRatePercent").value(50.0))
                .andExpect(jsonPath("$.summary.onTimeComplianceRatePercent").value(0.0));
    }

    @Test
    void dashboardValidatesParametersAndRequiresManagerAuthority() throws Exception {
        var manager = user("dashboard-validation-manager@example.com", UserRole.MANAGER);
        var member = user("dashboard-validation-member@example.com", UserRole.TEAM_MEMBER);

        mockMvc.perform(get("/api/v1/manager/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager)))
                        .param("weekStart", "2026-08-18"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/manager/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager)))
                        .param("weekStart", "2026-08-17")
                        .param("trendWeeks", "27"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/manager/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(member)))
                        .param("weekStart", "2026-08-17"))
                .andExpect(status().isForbidden());
    }
}
