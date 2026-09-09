package com.weeklyreport.report;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.ManagerTeamMember;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.ManagerTeamMemberRepository;

class TeamMemberProfileIntegrationTest extends ReportIntegrationTestSupport {

    @Autowired
    private ManagerTeamMemberRepository managerTeamMemberRepository;

    @Test
    void managerShouldSeeWholeTeamProfileStatisticsAndPaginatedHistory() throws Exception {
        var manager = user("profile-manager@example.com", UserRole.MANAGER);
        var member = user("profile-member@example.com", UserRole.TEAM_MEMBER);
        var inactiveMember = user("profile-inactive@example.com", UserRole.TEAM_MEMBER);
        inactiveMember.deactivate();
        userRepository.saveAndFlush(inactiveMember);
        assign(manager, member);
        assign(manager, inactiveMember);

        createReport(login(member), LocalDate.of(2026, 8, 3));

        UUID submitted = createPopulatedReport(member, LocalDate.of(2026, 8, 10));
        submit(member, submitted);

        UUID needsCorrection = createPopulatedReport(member, LocalDate.of(2026, 8, 17));
        submit(member, needsCorrection);
        review(manager, needsCorrection, "CHANGES_REQUESTED", "Add the missing details");

        UUID approved = createPopulatedReport(member, LocalDate.of(2026, 8, 24));
        submit(member, approved);
        review(manager, approved, "APPROVED", null);

        String managerToken = login(manager);

        mockMvc.perform(get("/api/v1/manager/team-members")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem(member.getId().toString())))
                .andExpect(jsonPath("$[*].id", hasItem(inactiveMember.getId().toString())));

        mockMvc.perform(get("/api/v1/manager/team-members/{memberId}", member.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.member.email").value(member.getEmail()))
                .andExpect(jsonPath("$.statistics.totalReports").value(4))
                .andExpect(jsonPath("$.statistics.draftReports").value(1))
                .andExpect(jsonPath("$.statistics.submittedReports").value(1))
                .andExpect(jsonPath("$.statistics.needsCorrectionReports").value(1))
                .andExpect(jsonPath("$.statistics.approvedReports").value(1));

        mockMvc.perform(get("/api/v1/manager/reports")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken))
                        .param("memberId", member.getId().toString())
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    void emptyProfileShouldReturnZeroStatistics() throws Exception {
        var manager = user("empty-profile-manager@example.com", UserRole.MANAGER);
        var member = user("empty-profile-member@example.com", UserRole.TEAM_MEMBER);
        assign(manager, member);

        mockMvc.perform(get("/api/v1/manager/team-members/{memberId}", member.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statistics.totalReports").value(0))
                .andExpect(jsonPath("$.statistics.draftReports").value(0))
                .andExpect(jsonPath("$.statistics.approvedReports").value(0));
    }

    @Test
    void memberShouldBeForbiddenAndNonMemberProfileShouldBeNotFound() throws Exception {
        var manager = user("profile-access-manager@example.com", UserRole.MANAGER);
        var member = user("profile-access-member@example.com", UserRole.TEAM_MEMBER);

        mockMvc.perform(get("/api/v1/manager/team-members/{memberId}", member.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(member))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/manager/team-members/{memberId}", manager.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager))))
                .andExpect(status().isNotFound());
    }

    @Test
    void managerCannotListOrOpenUnassignedMemberButAdminCan() throws Exception {
        var manager = user("scoped-profile-manager@example.com", UserRole.MANAGER);
        var assigned = user("scoped-profile-assigned@example.com", UserRole.TEAM_MEMBER);
        var unassigned = user("scoped-profile-unassigned@example.com", UserRole.TEAM_MEMBER);
        var admin = user("scoped-profile-admin@example.com", UserRole.ADMIN);
        assign(manager, assigned);

        String managerToken = login(manager);
        mockMvc.perform(get("/api/v1/manager/team-members")
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(assigned.getId().toString()))
                .andExpect(jsonPath("$[1]").doesNotExist());

        mockMvc.perform(get("/api/v1/manager/team-members/{memberId}", unassigned.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(managerToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/manager/team-members/{memberId}", unassigned.getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(admin))))
                .andExpect(status().isOk());
    }

    private void submit(com.weeklyreport.user.entity.User member, UUID reportId) throws Exception {
        mockMvc.perform(post("/api/v1/reports/{reportId}/submit", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(member))))
                .andExpect(status().isOk());
    }

    private UUID createPopulatedReport(
            com.weeklyreport.user.entity.User member,
            LocalDate weekStart
    ) throws Exception {
        String token = login(member);
        UUID reportId = createReport(token, weekStart);
        update(token, reportId, "{\"entityVersion\":0,\"notes\":\"Weekly progress\"}")
                .andExpect(status().isOk());
        return reportId;
    }

    private void review(
            com.weeklyreport.user.entity.User manager,
            UUID reportId,
            String action,
            String comment
    ) throws Exception {
        String commentProperty = comment == null
                ? ""
                : ",\"comment\":\"" + comment + "\"";
        mockMvc.perform(post("/api/v1/manager/reports/{reportId}/reviews", reportId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(login(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"%s\"%s}".formatted(action, commentProperty)))
                .andExpect(status().isOk());
    }

    private void assign(User manager, User member) {
        managerTeamMemberRepository.saveAndFlush(new ManagerTeamMember(member, manager));
    }
}
