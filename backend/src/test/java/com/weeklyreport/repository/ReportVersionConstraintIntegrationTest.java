package com.weeklyreport.repository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.report.entity.WeeklyReport;
import com.weeklyreport.report.repository.ReportVersionRepository;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@SpringBootTest
@Transactional
class ReportVersionConstraintIntegrationTest
        extends PostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WeeklyReportRepository weeklyReportRepository;

    @Autowired
    private ReportVersionRepository reportVersionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User teamMember;

    @BeforeEach
    void setUp() {
        teamMember = new User(
                "version-member@example.com",
                "test-hash",
                "Version",
                "Member",
                UserRole.TEAM_MEMBER
        );

        teamMember = userRepository.saveAndFlush(teamMember);
    }

    @Test
    void shouldAllowMultipleVersionsForSameReport() {
        WeeklyReport report = createReport(
                LocalDate.of(2026, 8, 31)
        );

        ReportVersion versionOne =
                new ReportVersion(report, 1, "Initial submission");

        ReportVersion versionTwo =
                new ReportVersion(report, 2, "Corrected submission");

        reportVersionRepository.saveAndFlush(versionOne);
        reportVersionRepository.saveAndFlush(versionTwo);

        var versions =
                reportVersionRepository
                        .findByReportIdOrderByVersionNumberDesc(
                                report.getId()
                        );

        assertEquals(2, versions.size());

        assertEquals(
                2,
                versions.getFirst().getVersionNumber()
        );

        assertEquals(
                1,
                versions.getLast().getVersionNumber()
        );
    }

    @Test
    void shouldRejectDuplicateVersionNumberForSameReport() {
        WeeklyReport report = createReport(
                LocalDate.of(2026, 8, 31)
        );

        ReportVersion firstVersion =
                new ReportVersion(report, 1, "First");

        reportVersionRepository.saveAndFlush(firstVersion);

        ReportVersion duplicateVersion =
                new ReportVersion(report, 1, "Duplicate");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> reportVersionRepository.saveAndFlush(
                        duplicateVersion
                )
        );
    }

    @Test
    void shouldRejectNonPositiveVersionNumber() {
        WeeklyReport report = createReport(
                LocalDate.of(2026, 8, 31)
        );

        ReportVersion invalidVersion =
                new ReportVersion(report, 0, "Invalid");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> reportVersionRepository.saveAndFlush(
                        invalidVersion
                )
        );
    }

    @Test
    void shouldPreventReportFromPointingToVersionOfAnotherReport() {
        WeeklyReport reportA = createReport(
                LocalDate.of(2026, 8, 31)
        );

        WeeklyReport reportB = createReport(
                LocalDate.of(2026, 9, 7)
        );

        ReportVersion versionA =
                new ReportVersion(reportA, 1, "Report A version");

        ReportVersion versionB =
                new ReportVersion(reportB, 1, "Report B version");

        reportVersionRepository.saveAndFlush(versionA);
        reportVersionRepository.saveAndFlush(versionB);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                        UPDATE weekly_report
                        SET current_version_id = ?
                        WHERE id = ?
                        """,
                        versionB.getId(),
                        reportA.getId()
                )
        );
    }

    private WeeklyReport createReport(LocalDate weekStart) {
        WeeklyReport report = new WeeklyReport(
                teamMember,
                weekStart
        );

        return weeklyReportRepository.saveAndFlush(report);
    }
}