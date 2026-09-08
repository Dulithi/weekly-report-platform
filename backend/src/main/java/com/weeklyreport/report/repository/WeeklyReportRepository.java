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

}
