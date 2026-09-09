package com.weeklyreport.report.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weeklyreport.report.content.Blocker;

public interface BlockerRepository
        extends JpaRepository<Blocker, UUID> {

    List<Blocker>
            findByReportVersionIdOrderBySortOrderAsc(
                    UUID reportVersionId
            );

    Optional<Blocker>
            findByReportVersionIdAndKeyBlockerTrue(
                    UUID reportVersionId
            );

    List<Blocker>
            findByReportVersionIdAndResolvedFalseOrderBySortOrderAsc(
                    UUID reportVersionId
            );

    long countByReportVersionIdAndResolvedFalse(
            UUID reportVersionId
    );

    @Query("""
            select blocker
            from Blocker blocker
            where blocker.reportVersion.id in :versionIds
            order by blocker.reportVersion.id asc, blocker.sortOrder asc
            """)
    List<Blocker> findForVersions(@Param("versionIds") Collection<UUID> versionIds);

    void deleteByReportVersionId(
            UUID reportVersionId
    );
}
