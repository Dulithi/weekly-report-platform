package com.weeklyreport.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import com.weeklyreport.project.repository.ProjectMemberRepository;
import com.weeklyreport.project.repository.ProjectRepository;
import com.weeklyreport.report.ReportStatus;
import com.weeklyreport.report.repository.ReportVersionRepository;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.report.repository.CompletedTaskRepository;
import com.weeklyreport.report.repository.PlannedTaskRepository;
import com.weeklyreport.report.repository.BlockerRepository;
import com.weeklyreport.report.repository.AchievementRepository;
import com.weeklyreport.report.repository.TimeEntryRepository;
import com.weeklyreport.review.repository.ReviewRepository;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.repository.ManagerTeamMemberRepository;
import com.weeklyreport.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "app.demo-seed.enabled=true",
        "app.demo-seed.password=SecureDemoPassword123!"
})
@Sql(
        statements = "TRUNCATE TABLE app_user CASCADE",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS,
        config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
)
class DemoDataSeederIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private DemoDataSeeder seeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private ManagerTeamMemberRepository managerTeamMemberRepository;

    @Autowired
    private WeeklyReportRepository weeklyReportRepository;

    @Autowired
    private ReportVersionRepository reportVersionRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CompletedTaskRepository completedTaskRepository;

    @Autowired
    private PlannedTaskRepository plannedTaskRepository;

    @Autowired
    private BlockerRepository blockerRepository;

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateCompleteRepeatableDemoDataset() throws Exception {
        var admin = userRepository.findByEmailIgnoreCase(DemoDataSeeder.ADMIN_EMAIL).orElseThrow();
        var manager = userRepository.findByEmailIgnoreCase(DemoDataSeeder.MANAGER_EMAIL).orElseThrow();

        assertThat(admin.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(manager.getRole()).isEqualTo(UserRole.MANAGER);
        assertThat(passwordEncoder.matches("SecureDemoPassword123!", admin.getPasswordHash())).isTrue();
        assertThat(userRepository.findAllByRoleOrderByLastNameAscFirstNameAscEmailAsc(
                UserRole.TEAM_MEMBER
        )).hasSize(5);
        assertThat(projectRepository.count()).isEqualTo(4);
        assertThat(projectMemberRepository.count()).isEqualTo(10);
        assertThat(managerTeamMemberRepository.findByManagerId(manager.getId())).hasSize(5);
        assertThat(weeklyReportRepository.count()).isEqualTo(19);
        assertThat(weeklyReportRepository.findByStatus(
                ReportStatus.DRAFT, org.springframework.data.domain.Pageable.unpaged()
        ).getTotalElements()).isEqualTo(1);
        assertThat(weeklyReportRepository.findByStatus(
                ReportStatus.SUBMITTED, org.springframework.data.domain.Pageable.unpaged()
        ).getTotalElements()).isEqualTo(2);
        assertThat(weeklyReportRepository.findByStatus(
                ReportStatus.NEEDS_CORRECTION, org.springframework.data.domain.Pageable.unpaged()
        ).getTotalElements()).isEqualTo(2);
        assertThat(weeklyReportRepository.findByStatus(
                ReportStatus.APPROVED, org.springframework.data.domain.Pageable.unpaged()
        ).getTotalElements()).isEqualTo(14);
        assertThat(reportVersionRepository.count()).isEqualTo(23);
        assertThat(reviewRepository.count()).isEqualTo(18);
        assertThat(completedTaskRepository.count()).isEqualTo(23);
        assertThat(plannedTaskRepository.count()).isEqualTo(23);
        assertThat(blockerRepository.count()).isEqualTo(23);
        assertThat(achievementRepository.count()).isEqualTo(23);
        assertThat(timeEntryRepository.count()).isEqualTo(92);

        seeder.run(new DefaultApplicationArguments(new String[0]));

        assertThat(userRepository.count()).isEqualTo(7);
        assertThat(projectRepository.count()).isEqualTo(4);
        assertThat(weeklyReportRepository.count()).isEqualTo(19);
        assertThat(reportVersionRepository.count()).isEqualTo(23);
        assertThat(reviewRepository.count()).isEqualTo(18);
        assertThat(completedTaskRepository.count()).isEqualTo(23);
        assertThat(timeEntryRepository.count()).isEqualTo(92);
    }
}
