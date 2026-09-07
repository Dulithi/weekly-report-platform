package com.weeklyreport.report.repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.weeklyreport.report.ReportStatus;
import com.weeklyreport.report.entity.WeeklyReport;

public interface WeeklyReportRepository
        extends JpaRepository<WeeklyReport, UUID>,
        JpaSpecificationExecutor<WeeklyReport> {

    Optional<WeeklyReport>
            findByIdAndUserId(
                    UUID reportId,
                    UUID userId
            );

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
}