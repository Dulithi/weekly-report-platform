package com.weeklyreport.report.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

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
}