package com.weeklyreport.report;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.weeklyreport.user.UserRole;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportComparisonIntegrationTest extends ReportIntegrationTestSupport {

    @Autowired
    private Clock clock;

    @Test
    void comparisonIncludesRosterButOnlyLatestSubmittedContent() throws Exception {
        var manager = user("comparison-manager@example.com", UserRole.MANAGER);
        var submittedMember = user("comparison-submitted@example.com", UserRole.TEAM_MEMBER);
        var draftMember = user("comparison-draft@example.com", UserRole.TEAM_MEMBER);
        assignManager(manager, submittedMember);
        assignManager(manager, draftMember);
        var missingMember = user("comparison-missing@example.com", UserRole.TEAM_MEMBER);
        var inactiveMember = user("comparison-inactive@example.com", UserRole.TEAM_MEMBER);
        assignManager(manager, missingMember);
        assignManager(manager, inactiveMember);
        LocalDate week = LocalDate.of(2099, 1, 5);

        String submittedToken = login(submittedMember);
        UUID submittedReport = createReport(submittedToken, week);
        update(submittedToken, submittedReport, """
                {
                  "entityVersion": 0,
                  "blockers": [
                    {"description":"Original key blocker","keyBlocker":true,"resolved":false},
                    {"description":"Resolved issue","keyBlocker":false,"resolved":true}
                  ],
                  "achievements": [
                    {"description":"Original highlight","keyAchievement":true},
                    {"description":"Secondary win","keyAchievement":false}
                  ]
                }
                """).andExpect(status().isOk());
        submit(submittedToken, submittedReport);

        String managerToken = login(manager);
        mockMvc.perform(post("/api/v1/manager/reports/{id}/reviews", submittedReport)
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"action":"CHANGES_REQUESTED","comment":"Clarify both sections"}
                                """))
                .andExpect(status().isOk());
        update(submittedToken, submittedReport, """
                {
                  "entityVersion": 0,
                  "blockers": [{"description":"Private correction blocker","keyBlocker":true,"resolved":false}],
                  "achievements": [{"description":"Private correction highlight","keyAchievement":true}]
                }
                """).andExpect(status().isOk());

        String draftToken = login(draftMember);
        UUID draftReport = createReport(draftToken, week);
        update(draftToken, draftReport, """
                {
                  "entityVersion": 0,
                  "blockers": [{"description":"Private draft blocker","keyBlocker":true,"resolved":false}],
                  "achievements": [{"description":"Private draft achievement","keyAchievement":true}]
                }
                """).andExpect(status().isOk());

        String inactiveToken = login(inactiveMember);
        UUID inactiveReport = createReport(inactiveToken, week);
        update(inactiveToken, inactiveReport,
                "{\"entityVersion\":0,\"blockers\":[{\"description\":\"Inactive blocker\",\"keyBlocker\":true,\"resolved\":false}]}")
                .andExpect(status().isOk());
        submit(inactiveToken, inactiveReport);
        inactiveMember.deactivate();
        userRepository.saveAndFlush(inactiveMember);

        mockMvc.perform(get("/api/v1/manager/report-comparisons")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .param("weekStart", week.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value(week.toString()))
                .andExpect(jsonPath("$.weekEnd").value(week.plusDays(6).toString()))
                .andExpect(jsonPath("$.members.length()").value(3))
                .andExpect(jsonPath("$.members[?(@.member.id == '%s')].status"
                        .formatted(submittedMember.getId())).value(hasItem("NEEDS_CORRECTION")))
                .andExpect(jsonPath("$.members[?(@.member.id == '%s')].submittedVersionNumber"
                        .formatted(submittedMember.getId())).value(hasItem(1)))
                .andExpect(jsonPath("$.members[?(@.member.id == '%s')].blockers[0].description"
                        .formatted(submittedMember.getId())).value(hasItem("Original key blocker")))
                .andExpect(jsonPath("$.members[?(@.member.id == '%s')].blockers[0].keyBlocker"
                        .formatted(submittedMember.getId())).value(hasItem(true)))
                .andExpect(jsonPath("$.members[?(@.member.id == '%s')].achievements[0].description"
                        .formatted(submittedMember.getId())).value(hasItem("Original highlight")))
                .andExpect(jsonPath("$.members[*].blockers[*].description")
                        .value(not(hasItem("Private correction blocker"))))
                .andExpect(jsonPath("$.members[*].blockers[*].description")
                        .value(not(hasItem("Private draft blocker"))))
                .andExpect(jsonPath("$.members[?(@.member.id == '%s')].blockers.length()"
                        .formatted(draftMember.getId())).value(hasItem(0)))
                .andExpect(jsonPath("$.members[?(@.member.id == '%s')].status"
                        .formatted(missingMember.getId())).value(hasItem("NOT_STARTED")))
                .andExpect(jsonPath("$.members[?(@.member.id == '%s')].submittedVersionNumber"
                        .formatted(missingMember.getId())).value(hasItem(org.hamcrest.Matchers.nullValue())))
                .andExpect(jsonPath("$.members[?(@.member.id == '%s')]"
                        .formatted(inactiveMember.getId())).isEmpty());
    }

    @Test
    void comparisonRequiresMondayAndManagerAuthority() throws Exception {
        var manager = user("comparison-validation-manager@example.com", UserRole.MANAGER);
        var member = user("comparison-validation-member@example.com", UserRole.TEAM_MEMBER);

        mockMvc.perform(get("/api/v1/manager/report-comparisons")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weekStart").value(LocalDate.now(clock)
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()));

        mockMvc.perform(get("/api/v1/manager/report-comparisons")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager)))
                        .param("weekStart", "2026-08-18"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/manager/report-comparisons")
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(member)))
                        .param("weekStart", "2026-08-17"))
                .andExpect(status().isForbidden());
    }

    private void submit(String token, UUID reportId) throws Exception {
        mockMvc.perform(post("/api/v1/reports/{id}/submit", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());
    }
}
