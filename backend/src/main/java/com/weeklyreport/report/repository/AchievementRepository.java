package com.weeklyreport.report.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.weeklyreport.report.content.Achievement;

public interface AchievementRepository
        extends JpaRepository<Achievement, UUID> {

    List<Achievement>
            findByReportVersionIdOrderBySortOrderAsc(
                    UUID reportVersionId
            );

    Optional<Achievement>
            findByReportVersionIdAndKeyAchievementTrue(
                    UUID reportVersionId
            );

    @Query("""
            select achievement
            from Achievement achievement
            where achievement.reportVersion.id in :versionIds
            order by achievement.reportVersion.id asc, achievement.sortOrder asc
            """)
    List<Achievement> findForVersions(@Param("versionIds") Collection<UUID> versionIds);

    void deleteByReportVersionId(
            UUID reportVersionId
    );
}
