package com.weeklyreport.report.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    Optional<ReportVersion>
            findTopByReportIdAndSubmittedAtIsNotNullOrderByVersionNumberDesc(
                    UUID reportId
            );

    @Query("""
            select version.report.id as reportId,
                   min(version.submittedAt) as firstSubmittedAt
            from ReportVersion version
            where version.report.id in :reportIds
              and version.submittedAt is not null
            group by version.report.id
            """)
    List<ReportFirstSubmissionProjection> findFirstSubmissions(
            @Param("reportIds") Collection<UUID> reportIds
    );
}
