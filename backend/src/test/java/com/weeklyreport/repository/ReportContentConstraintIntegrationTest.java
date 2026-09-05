package com.weeklyreport.repository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.report.TaskType;
import com.weeklyreport.report.content.Achievement;
import com.weeklyreport.report.content.Blocker;
import com.weeklyreport.report.content.TimeEntry;
import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.report.entity.WeeklyReport;
import com.weeklyreport.report.repository.AchievementRepository;
import com.weeklyreport.report.repository.BlockerRepository;
import com.weeklyreport.report.repository.ReportVersionRepository;
import com.weeklyreport.report.repository.TimeEntryRepository;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@SpringBootTest
@Transactional
class ReportContentConstraintIntegrationTest
        extends PostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WeeklyReportRepository weeklyReportRepository;

    @Autowired
    private ReportVersionRepository reportVersionRepository;

    @Autowired
    private BlockerRepository blockerRepository;

    @Autowired
    private AchievementRepository achievementRepository;

    @Autowired
    private TimeEntryRepository timeEntryRepository;

    private ReportVersion reportVersion;

    @BeforeEach
    void setUp() {
        User teamMember = new User(
                "content-member@example.com",
                "test-hash",
                "Content",
                "Member",
                UserRole.TEAM_MEMBER
        );

        teamMember = userRepository.saveAndFlush(teamMember);

        WeeklyReport report = new WeeklyReport(
                teamMember,
                LocalDate.of(2026, 8, 31)
        );

        report = weeklyReportRepository.saveAndFlush(report);

        reportVersion = new ReportVersion(
                report,
                1,
                "Initial version"
        );

        reportVersion =
                reportVersionRepository.saveAndFlush(reportVersion);
    }

    @Test
    void shouldRejectMultipleKeyBlockersForSameVersion() {
        Blocker firstBlocker = new Blocker(
                reportVersion,
                "Test environment unavailable",
                true,
                false,
                0
        );

        blockerRepository.saveAndFlush(firstBlocker);

        Blocker secondBlocker = new Blocker(
                reportVersion,
                "API access delayed",
                true,
                false,
                1
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> blockerRepository.saveAndFlush(
                        secondBlocker
                )
        );
    }

    @Test
    void shouldAllowMultipleNonKeyBlockers() {
        Blocker firstBlocker = new Blocker(
                reportVersion,
                "Test environment unavailable",
                false,
                false,
                0
        );

        Blocker secondBlocker = new Blocker(
                reportVersion,
                "API access delayed",
                false,
                false,
                1
        );

        blockerRepository.saveAndFlush(firstBlocker);
        blockerRepository.saveAndFlush(secondBlocker);
    }

    @Test
    void shouldRejectMultipleKeyAchievementsForSameVersion() {
        Achievement firstAchievement = new Achievement(
                reportVersion,
                "Completed authentication module",
                true,
                0
        );

        achievementRepository.saveAndFlush(firstAchievement);

        Achievement secondAchievement = new Achievement(
                reportVersion,
                "Completed reporting module",
                true,
                1
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> achievementRepository.saveAndFlush(
                        secondAchievement
                )
        );
    }

    @Test
    void shouldRejectDuplicateTimeTypeForSameVersion() {
        TimeEntry firstEntry = new TimeEntry(
                reportVersion,
                TaskType.DEVELOPMENT,
                300
        );

        timeEntryRepository.saveAndFlush(firstEntry);

        TimeEntry duplicateEntry = new TimeEntry(
                reportVersion,
                TaskType.DEVELOPMENT,
                120
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> timeEntryRepository.saveAndFlush(
                        duplicateEntry
                )
        );
    }

    @Test
    void shouldRejectNegativeTimeEntry() {
        TimeEntry invalidEntry = new TimeEntry(
                reportVersion,
                TaskType.TESTING,
                -30
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> timeEntryRepository.saveAndFlush(
                        invalidEntry
                )
        );
    }
}