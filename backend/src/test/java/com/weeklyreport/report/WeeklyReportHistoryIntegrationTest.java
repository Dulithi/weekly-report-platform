package com.weeklyreport.report;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weeklyreport.user.UserRole;

class WeeklyReportHistoryIntegrationTest extends ReportIntegrationTestSupport {

    @Test
    void historyIsPagedNewestFirstAndContainsOnlyTheAuthenticatedUsersSummaries() throws Exception {
        var member = user("history-member@example.com", UserRole.TEAM_MEMBER);
        var other = user("history-other@example.com", UserRole.TEAM_MEMBER);
        String token = login(member);

        createReport(token, LocalDate.of(2026, 8, 17));
        createReport(token, LocalDate.of(2026, 8, 24));
        createReport(token, LocalDate.of(2026, 8, 31));
        createReport(login(other), LocalDate.of(2026, 8, 31));

        mockMvc.perform(get(
                "/api/v1/reports/me"
        ).param("page", "0").param("size", "2")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content[0].weekStart").value("2026-08-31"))
            .andExpect(jsonPath("$.content[1].weekStart").value("2026-08-24"))
            .andExpect(jsonPath("$.content[0].currentVersion").doesNotExist())
            .andExpect(jsonPath("$.content[0].completedTasks").doesNotExist());
    }
    
    @Test
    void anonymousUserCannotAccessReportHistory()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/reports/me")
        )
        .andExpect(
                status().isUnauthorized()
        );
    }
}