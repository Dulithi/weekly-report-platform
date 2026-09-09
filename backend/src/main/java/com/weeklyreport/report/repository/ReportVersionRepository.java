package com.weeklyreport.report.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.user.UserRole;

public interface ReportVersionRepository
        extends JpaRepository<ReportVersion, UUID> {

    List<ReportVersion>
            findByReportIdOrderByVersionNumberAsc(
                    UUID reportId
            );

    List<ReportVersion> findByReportIdAndSubmittedAtIsNotNullOrderByVersionNumberAsc(
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

    Optional<ReportVersion> findByReportIdAndVersionNumberAndSubmittedAtIsNotNull(
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

    @Query("""
            select version
            from ReportVersion version
            join fetch version.report report
            join fetch report.user member
            where report.weekStart = :weekStart
              and member.active = true
              and member.role = :role
              and member.id in :memberIds
              and version.submittedAt is not null
              and version.versionNumber = (
                  select max(candidate.versionNumber)
                  from ReportVersion candidate
                  where candidate.report.id = report.id
                    and candidate.submittedAt is not null
              )
            order by member.lastName asc, member.firstName asc, member.email asc
            """)
    List<ReportVersion> findLatestSubmittedForActiveMembers(
            @Param("weekStart") LocalDate weekStart,
            @Param("role") UserRole role,
            @Param("memberIds") java.util.Collection<UUID> memberIds
    );

    @Query("""
            select version
            from ReportVersion version
            join fetch version.report report
            join fetch report.user member
            where report.weekStart between :from and :to
              and member.active = true
              and member.role = :role
              and member.id in :memberIds
              and version.submittedAt is not null
              and version.versionNumber = (
                  select max(candidate.versionNumber)
                  from ReportVersion candidate
                  where candidate.report.id = report.id
                    and candidate.submittedAt is not null
              )
            order by report.weekStart desc,
                     member.lastName asc, member.firstName asc, member.email asc
            """)
    List<ReportVersion> findLatestSubmittedForActiveMembersBetween(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("role") UserRole role,
            @Param("memberIds") java.util.Collection<UUID> memberIds,
            Pageable pageable
    );
}
