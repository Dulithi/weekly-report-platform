package com.weeklyreport.report.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weeklyreport.report.content.PlannedTask;

public interface PlannedTaskRepository extends JpaRepository<PlannedTask, UUID> {

    boolean existsByProjectId(UUID projectId);

    List<PlannedTask> findByReportVersionIdOrderBySortOrderAsc(
            UUID reportVersionId
    );

    List<PlannedTask> findByReportVersionIdAndProjectIdOrderBySortOrderAsc(
            UUID reportVersionId,
            UUID projectId
    );

    @Query("""
            select task from PlannedTask task
            left join fetch task.project
            where task.reportVersion.id in :versionIds
            order by task.reportVersion.id asc, task.sortOrder asc
            """)
    List<PlannedTask> findForVersions(@Param("versionIds") Collection<UUID> versionIds);

    void deleteByReportVersionId(UUID reportVersionId);
}
