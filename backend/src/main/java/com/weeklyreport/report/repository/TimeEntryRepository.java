package com.weeklyreport.report.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            select entry from TimeEntry entry
            where entry.reportVersion.id in :versionIds
            order by entry.reportVersion.id asc, entry.taskType asc
            """)
    List<TimeEntry> findForVersions(@Param("versionIds") Collection<UUID> versionIds);

    void deleteByReportVersionId(
            UUID reportVersionId
    );
}
