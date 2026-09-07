package com.weeklyreport.report.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.weeklyreport.report.ReportStatus;
import com.weeklyreport.report.content.CompletedTask;
import com.weeklyreport.report.content.PlannedTask;
import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.report.entity.WeeklyReport;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public final class ManagerReportSpecifications {

    private ManagerReportSpecifications() {
    }

    public static Specification<WeeklyReport> filteredBy(
            UUID memberId,
            UUID projectId,
            LocalDate earliestWeekStart,
            LocalDate latestWeekStart,
            ReportStatus status
    ) {
        return (report, query, builder) -> {
            if (!Long.class.equals(query.getResultType())) {
                report.fetch("user", JoinType.INNER);
            }

            List<Predicate> predicates = new ArrayList<>();
            if (memberId != null) {
                predicates.add(builder.equal(report.get("user").get("id"), memberId));
            }
            if (earliestWeekStart != null) {
                predicates.add(builder.greaterThanOrEqualTo(
                        report.get("weekStart"), earliestWeekStart
                ));
            }
            if (latestWeekStart != null) {
                predicates.add(builder.lessThanOrEqualTo(report.get("weekStart"), latestWeekStart));
            }
            if (status != null) {
                predicates.add(builder.equal(report.get("status"), status));
            }
            if (projectId != null) {
                predicates.add(hasProjectInLatestSubmittedVersion(
                        report, query.subquery(Integer.class), query.subquery(Integer.class),
                        query.subquery(Integer.class), projectId, builder
                ));
            }

            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate hasProjectInLatestSubmittedVersion(
            Root<WeeklyReport> report,
            Subquery<Integer> latestVersion,
            Subquery<Integer> completedExists,
            Subquery<Integer> plannedExists,
            UUID projectId,
            jakarta.persistence.criteria.CriteriaBuilder builder
    ) {
        Root<ReportVersion> version = latestVersion.from(ReportVersion.class);
        latestVersion.select(builder.max(version.get("versionNumber")))
                .where(
                        builder.equal(version.get("report"), report),
                        builder.isNotNull(version.get("submittedAt"))
                );

        Root<CompletedTask> completed = completedExists.from(CompletedTask.class);
        completedExists.select(builder.literal(1)).where(
                builder.equal(completed.get("reportVersion").get("report"), report),
                builder.equal(completed.get("project").get("id"), projectId),
                builder.isNotNull(completed.get("reportVersion").get("submittedAt")),
                builder.equal(
                        completed.get("reportVersion").get("versionNumber"), latestVersion
                )
        );

        Root<PlannedTask> planned = plannedExists.from(PlannedTask.class);
        plannedExists.select(builder.literal(1)).where(
                builder.equal(planned.get("reportVersion").get("report"), report),
                builder.equal(planned.get("project").get("id"), projectId),
                builder.isNotNull(planned.get("reportVersion").get("submittedAt")),
                builder.equal(planned.get("reportVersion").get("versionNumber"), latestVersion)
        );

        return builder.or(builder.exists(completedExists), builder.exists(plannedExists));
    }
}
