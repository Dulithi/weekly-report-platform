package com.weeklyreport.report.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weeklyreport.report.ReportStatus;
import com.weeklyreport.report.entity.WeeklyReport;
import com.weeklyreport.dashboard.repository.CompletedTaskTrendProjection;
import com.weeklyreport.dashboard.repository.DashboardContentMetricsProjection;
import com.weeklyreport.dashboard.repository.ProjectTaskDistributionProjection;
import com.weeklyreport.dashboard.repository.TaskTypeTimeProjection;

import jakarta.persistence.LockModeType;

public interface WeeklyReportRepository
        extends JpaRepository<WeeklyReport, UUID>,
        JpaSpecificationExecutor<WeeklyReport> {

    Optional<WeeklyReport>
            findByIdAndUserId(
                    UUID reportId,
                    UUID userId
            );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select report from WeeklyReport report where report.id = :reportId")
    Optional<WeeklyReport> findByIdForReview(@Param("reportId") UUID reportId);

    Optional<WeeklyReport>
            findByUserIdAndWeekStart(
                    UUID userId,
                    LocalDate weekStart
            );

    boolean existsByUserIdAndWeekStart(
            UUID userId,
            LocalDate weekStart
    );

    Page<WeeklyReport>
            findByUserId(
                    UUID userId,
                    Pageable pageable
            );

    Page<WeeklyReport>
            findByStatus(
                    ReportStatus status,
                    Pageable pageable
            );

    @EntityGraph(attributePaths = "user")
    List<WeeklyReport> findAllByUserIdInAndWeekStart(
            Collection<UUID> userIds,
            LocalDate weekStart
    );

    @Query("select report.id from WeeklyReport report where report.user.id in :userIds")
    List<UUID> findIdsByUserIdIn(@Param("userIds") Collection<UUID> userIds);

    @Query(value = """
            SELECT
                COUNT(*) AS total_reports,
                COUNT(*) FILTER (WHERE status = 'DRAFT') AS draft_reports,
                COUNT(*) FILTER (WHERE status = 'SUBMITTED') AS submitted_reports,
                COUNT(*) FILTER (WHERE status = 'NEEDS_CORRECTION') AS needs_correction_reports,
                COUNT(*) FILTER (WHERE status = 'APPROVED') AS approved_reports
            FROM weekly_report
            WHERE user_id = :userId
            """, nativeQuery = true)
    TeamMemberReportStatisticsProjection getStatisticsForUser(
            @Param("userId") UUID userId
    );

    @Query(value = """
            WITH latest_submitted AS (
                SELECT id, report_id
                FROM (
                    SELECT rv.id, rv.report_id,
                           ROW_NUMBER() OVER (
                               PARTITION BY rv.report_id
                               ORDER BY rv.version_number DESC
                           ) AS row_number
                    FROM report_version rv
                    JOIN weekly_report wr ON wr.id = rv.report_id
                    JOIN app_user member ON member.id = wr.user_id
                    WHERE wr.week_start = :weekStart
                      AND rv.submitted_at IS NOT NULL
                      AND member.active = TRUE
                      AND member.role = 'TEAM_MEMBER'
                      AND member.id IN (:memberIds)
                ) ranked
                WHERE row_number = 1
            )
            SELECT
                (SELECT COUNT(*)
                 FROM weekly_report wr
                 JOIN app_user member ON member.id = wr.user_id
                 WHERE wr.week_start = :weekStart
                   AND wr.status = 'NEEDS_CORRECTION'
                   AND member.active = TRUE
                   AND member.role = 'TEAM_MEMBER'
                   AND member.id IN (:memberIds)) AS needs_correction_reports,
                (SELECT COUNT(*)
                 FROM blocker b
                 JOIN latest_submitted latest ON latest.id = b.report_version_id
                 WHERE b.resolved = FALSE) AS open_blockers
            """, nativeQuery = true)
    DashboardContentMetricsProjection getDashboardContentMetrics(
            @Param("weekStart") LocalDate weekStart,
            @Param("memberIds") Collection<UUID> memberIds
    );

    @Query(value = """
            WITH latest_submitted AS (
                SELECT id
                FROM (
                    SELECT rv.id,
                           ROW_NUMBER() OVER (
                               PARTITION BY rv.report_id
                               ORDER BY rv.version_number DESC
                           ) AS row_number
                    FROM report_version rv
                    JOIN weekly_report wr ON wr.id = rv.report_id
                    JOIN app_user member ON member.id = wr.user_id
                    WHERE wr.week_start = :weekStart
                      AND rv.submitted_at IS NOT NULL
                      AND member.active = TRUE
                      AND member.role = 'TEAM_MEMBER'
                      AND member.id IN (:memberIds)
                ) ranked
                WHERE row_number = 1
            )
            SELECT p.id AS project_id,
                   COALESCE(p.name, 'Uncategorized') AS project_name,
                   COUNT(*) AS task_count
            FROM completed_task task
            JOIN latest_submitted latest ON latest.id = task.report_version_id
            LEFT JOIN project p ON p.id = task.project_id
            GROUP BY p.id, p.name
            ORDER BY task_count DESC, project_name ASC
            """, nativeQuery = true)
    List<ProjectTaskDistributionProjection> getProjectTaskDistribution(
            @Param("weekStart") LocalDate weekStart,
            @Param("memberIds") Collection<UUID> memberIds
    );

    @Query(value = """
            WITH latest_submitted AS (
                SELECT id
                FROM (
                    SELECT rv.id,
                           ROW_NUMBER() OVER (
                               PARTITION BY rv.report_id
                               ORDER BY rv.version_number DESC
                           ) AS row_number
                    FROM report_version rv
                    JOIN weekly_report wr ON wr.id = rv.report_id
                    JOIN app_user member ON member.id = wr.user_id
                    WHERE wr.week_start = :weekStart
                      AND rv.submitted_at IS NOT NULL
                      AND member.active = TRUE
                      AND member.role = 'TEAM_MEMBER'
                      AND member.id IN (:memberIds)
                ) ranked
                WHERE row_number = 1
            )
            SELECT entry.task_type AS task_type,
                   SUM(entry.minutes) AS minutes
            FROM time_entry entry
            JOIN latest_submitted latest ON latest.id = entry.report_version_id
            GROUP BY entry.task_type
            ORDER BY minutes DESC, task_type ASC
            """, nativeQuery = true)
    List<TaskTypeTimeProjection> getTimeByTaskType(
            @Param("weekStart") LocalDate weekStart,
            @Param("memberIds") Collection<UUID> memberIds
    );

    @Query(value = """
            WITH latest_submitted AS (
                SELECT id, report_id
                FROM (
                    SELECT rv.id, rv.report_id,
                           ROW_NUMBER() OVER (
                               PARTITION BY rv.report_id
                               ORDER BY rv.version_number DESC
                           ) AS row_number
                    FROM report_version rv
                    JOIN weekly_report wr ON wr.id = rv.report_id
                    JOIN app_user member ON member.id = wr.user_id
                    WHERE wr.week_start BETWEEN :from AND :to
                      AND rv.submitted_at IS NOT NULL
                      AND member.active = TRUE
                      AND member.role = 'TEAM_MEMBER'
                      AND member.id IN (:memberIds)
                ) ranked
                WHERE row_number = 1
            )
            SELECT wr.week_start AS week_start,
                   COUNT(task.id) AS completed_tasks
            FROM latest_submitted latest
            JOIN weekly_report wr ON wr.id = latest.report_id
            JOIN completed_task task ON task.report_version_id = latest.id
            WHERE task.status = 'COMPLETED'
            GROUP BY wr.week_start
            ORDER BY wr.week_start ASC
            """, nativeQuery = true)
    List<CompletedTaskTrendProjection> getCompletedTaskTrend(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("memberIds") Collection<UUID> memberIds
    );

}
