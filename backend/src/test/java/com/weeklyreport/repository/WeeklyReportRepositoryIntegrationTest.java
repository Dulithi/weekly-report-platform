package com.weeklyreport.repository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.report.entity.WeeklyReport;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@SpringBootTest
@Transactional
class WeeklyReportRepositoryIntegrationTest
        extends PostgresIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WeeklyReportRepository weeklyReportRepository;

    private User teamMember;

    @BeforeEach
    void setUp() {
        teamMember = new User(
                "weekly-report-member@example.com",
                "test-hash",
                "Weekly",
                "Member",
                UserRole.TEAM_MEMBER
        );

        teamMember = userRepository.saveAndFlush(teamMember);
    }

    @Test
    void shouldCreateOneWeeklyReportForUserAndWeek() {
        LocalDate monday = LocalDate.of(2026, 8, 31);

        WeeklyReport report = new WeeklyReport(
                teamMember,
                monday
        );

        WeeklyReport saved =
                weeklyReportRepository.saveAndFlush(report);

        assertNotNull(saved.getId());
        assertEquals(monday, saved.getWeekStart());

        assertEquals(
                LocalDate.of(2026, 9, 6),
                saved.getWeekEnd()
        );
    }

    @Test
    void shouldRejectSecondReportForSameUserAndWeek() {
        LocalDate monday = LocalDate.of(2026, 8, 31);

        WeeklyReport firstReport = new WeeklyReport(
                teamMember,
                monday
        );

        weeklyReportRepository.saveAndFlush(firstReport);

        WeeklyReport duplicateReport = new WeeklyReport(
                teamMember,
                monday
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> weeklyReportRepository.saveAndFlush(
                        duplicateReport
                )
        );
    }

    @Test
    void shouldAllowSameUserToCreateReportsForDifferentWeeks() {
        WeeklyReport weekOne = new WeeklyReport(
                teamMember,
                LocalDate.of(2026, 8, 31)
        );

        WeeklyReport weekTwo = new WeeklyReport(
                teamMember,
                LocalDate.of(2026, 9, 7)
        );

        weeklyReportRepository.saveAndFlush(weekOne);
        weeklyReportRepository.saveAndFlush(weekTwo);

        assertNotNull(weekOne.getId());
        assertNotNull(weekTwo.getId());

        assertNotEquals(
                weekOne.getId(),
                weekTwo.getId()
        );
    }

    @Test
    void shouldRejectWeekStartThatIsNotMonday() {
        LocalDate tuesday = LocalDate.of(2026, 9, 1);

        WeeklyReport report = new WeeklyReport(
                teamMember,
                tuesday
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> weeklyReportRepository.saveAndFlush(report)
        );
    }
}