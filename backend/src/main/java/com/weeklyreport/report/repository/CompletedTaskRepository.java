package com.weeklyreport.report.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreport.report.content.CompletedTask;

public interface CompletedTaskRepository extends JpaRepository<CompletedTask, UUID> {

    List<CompletedTask>
            findByReportVersionIdOrderBySortOrderAsc(
                    UUID reportVersionId
            );

    List<CompletedTask>
            findByReportVersionIdAndProjectIdOrderBySortOrderAsc(
                    UUID reportVersionId,
                    UUID projectId
            );

}