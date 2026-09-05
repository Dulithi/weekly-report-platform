package com.weeklyreport.report.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

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
}