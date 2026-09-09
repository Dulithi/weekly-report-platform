package com.weeklyreport.report;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.weeklyreport.common.exception.ConflictException;
import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.report.entity.WeeklyReport;
import com.weeklyreport.report.repository.ReportVersionRepository;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.review.repository.ReviewRepository;
import com.weeklyreport.review.service.ReviewService;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.entity.ManagerTeamMember;
import com.weeklyreport.user.repository.ManagerTeamMemberRepository;
import com.weeklyreport.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ManagerReviewConcurrencyIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ManagerTeamMemberRepository managerTeamMemberRepository;

    @Autowired
    private WeeklyReportRepository weeklyReportRepository;

    @Autowired
    private ReportVersionRepository reportVersionRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID reportId;
    private List<UUID> userIds = List.of();

    @Test
    void simultaneousManagersCannotBothReviewTheSameSubmission() throws Exception {
        var fixture = transactionTemplate.execute(ignored -> createSubmittedReport());
        reportId = fixture.reportId();
        userIds = List.of(fixture.memberId(), fixture.firstManagerId(), fixture.secondManagerId());
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> approveAfterStart(
                    start, fixture.firstManagerId(), "First review"
            ));
            var second = executor.submit(() -> approveAfterStart(
                    start, fixture.secondManagerId(), "Second review"
            ));
            start.countDown();

            assertThat(List.of(first.get(), second.get()))
                    .containsExactlyInAnyOrder("APPROVED", "CONFLICT");
        }

        assertThat(reviewRepository.findByReportVersionReportIdOrderByCreatedAtAsc(reportId))
                .hasSize(1);
        assertThat(weeklyReportRepository.findById(reportId).orElseThrow().getStatus())
                .isEqualTo(ReportStatus.APPROVED);
    }

    private String approveAfterStart(
            CountDownLatch start,
            UUID reviewerId,
            String comment
    ) throws InterruptedException {
        start.await();
        try {
            reviewService.approve(reportId, reviewerId, comment);
            return "APPROVED";
        } catch (ConflictException exception) {
            return "CONFLICT";
        }
    }

    private Fixture createSubmittedReport() {
        User member = userRepository.save(new User(
                "review-race-member@example.com", "unused", "Race", "Member",
                UserRole.TEAM_MEMBER
        ));
        User firstManager = userRepository.save(new User(
                "review-race-manager-one@example.com", "unused", "First", "Manager",
                UserRole.MANAGER
        ));
        userRepository.flush();
        managerTeamMemberRepository.save(new ManagerTeamMember(member, firstManager));
        managerTeamMemberRepository.flush();

        WeeklyReport report = weeklyReportRepository.saveAndFlush(
                new WeeklyReport(member, LocalDate.of(2026, 8, 17))
        );
        ReportVersion version = new ReportVersion(report, 1, "Ready");
        Instant submittedAt = Instant.parse("2026-08-23T12:00:00Z");
        version.markSubmitted(submittedAt);
        reportVersionRepository.saveAndFlush(version);
        report.setCurrentVersion(version);
        report.submit(submittedAt);
        weeklyReportRepository.saveAndFlush(report);

        return new Fixture(
                report.getId(), member.getId(), firstManager.getId(), firstManager.getId()
        );
    }

    @AfterEach
    void cleanCommittedFixture() {
        if (reportId == null) {
            return;
        }
        try {
            jdbcTemplate.update("delete from activity_log where entity_id = ?", reportId);
            jdbcTemplate.update("delete from report_status_history where report_id = ?", reportId);
            jdbcTemplate.update(
                    "delete from review where report_version_id in "
                            + "(select id from report_version where report_id = ?)",
                    reportId
            );
            jdbcTemplate.update(
                    "update weekly_report set current_version_id = null where id = ?", reportId
            );
            jdbcTemplate.update("delete from report_version where report_id = ?", reportId);
            jdbcTemplate.update("delete from weekly_report where id = ?", reportId);
            jdbcTemplate.update("delete from manager_team_member where team_member_id = ?", userIds.getFirst());
            for (UUID userId : userIds) {
                jdbcTemplate.update("delete from app_user where id = ?", userId);
            }
        } catch (DataAccessException ignored) {
            // Preserve the original test failure; Testcontainers discards this database after the suite.
        }
    }

    private record Fixture(
            UUID reportId,
            UUID memberId,
            UUID firstManagerId,
            UUID secondManagerId
    ) {
    }
}
