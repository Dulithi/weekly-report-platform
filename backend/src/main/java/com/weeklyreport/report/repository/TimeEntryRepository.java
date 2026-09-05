package com.weeklyreport.report.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreport.report.TaskType;
import com.weeklyreport.report.content.TimeEntry;

public interface TimeEntryRepository
        extends JpaRepository<TimeEntry, UUID> {

    List<TimeEntry>
            findByReportVersionId(
                    UUID reportVersionId
            );

    Optional<TimeEntry>
            findByReportVersionIdAndTaskType(
                    UUID reportVersionId,
                    TaskType taskType
            );
}