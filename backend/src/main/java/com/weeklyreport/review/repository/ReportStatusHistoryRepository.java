package com.weeklyreport.review.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreport.review.entity.ReportStatusHistory;

public interface ReportStatusHistoryRepository
        extends JpaRepository<ReportStatusHistory, UUID> {

    List<ReportStatusHistory>
            findByReportIdOrderByChangedAtAsc(
                    UUID reportId
            );

    Optional<ReportStatusHistory>
            findFirstByReportIdOrderByChangedAtDesc(
                    UUID reportId
            );
}