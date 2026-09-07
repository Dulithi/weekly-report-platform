package com.weeklyreport.report;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.weeklyreport.user.UserRole;

class WeeklyReportOwnershipIntegrationTest extends ReportIntegrationTestSupport {

    @Test
    void anotherMemberCannotReadUpdateOrSubmitTheReport() throws Exception {
        var alice = user("ownership-alice@example.com", UserRole.TEAM_MEMBER);
        var bob = user("ownership-bob@example.com", UserRole.TEAM_MEMBER);
        String aliceToken = login(alice);
        UUID reportId = createReport(aliceToken, LocalDate.of(2026, 8, 17));
        String bobToken = login(bob);

        getReport(bobToken, reportId).andExpect(status().isNotFound());
        update(bobToken, reportId, "{\"entityVersion\":0}")
                .andExpect(status().isNotFound());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                "/api/v1/reports/{reportId}/submit", reportId
        ).header("Authorization", bearer(bobToken))).andExpect(status().isNotFound());
    }

    @Test
    void managerCannotUseMemberReportEndpoint() throws Exception {
        var member = user("ownership-member@example.com", UserRole.TEAM_MEMBER);
        var manager = user("ownership-manager@example.com", UserRole.MANAGER);
        UUID reportId = createReport(login(member), LocalDate.of(2026, 8, 17));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/v1/reports/{reportId}", reportId
        ).header("Authorization", bearer(login(manager))))
            .andExpect(status().isForbidden());
    }
}