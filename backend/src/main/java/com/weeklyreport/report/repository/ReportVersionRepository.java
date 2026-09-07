package com.weeklyreport.report.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreport.report.entity.ReportVersion;

public interface ReportVersionRepository
        extends JpaRepository<ReportVersion, UUID> {

    List<ReportVersion>
            findByReportIdOrderByVersionNumberAsc(
                    UUID reportId
            );

    List<ReportVersion>
            findByReportIdOrderByVersionNumberDesc(
                    UUID reportId
            );

    Optional<ReportVersion>
            findByReportIdAndVersionNumber(
                    UUID reportId,
                    int versionNumber
            );

    Optional<ReportVersion>
            findTopByReportIdOrderByVersionNumberDesc(
                    UUID reportId
            );
}