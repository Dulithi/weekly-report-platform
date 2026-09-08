package com.weeklyreport.report.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

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

    void deleteByReportVersionId(UUID reportVersionId);
}
