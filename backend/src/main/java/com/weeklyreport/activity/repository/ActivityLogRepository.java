package com.weeklyreport.activity.repository;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.weeklyreport.activity.ActivityType;
import com.weeklyreport.activity.entity.ActivityLog;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {

    Page<ActivityLog> findByOrderByCreatedAtDesc(
            Pageable pageable
    );

    Page<ActivityLog> findByActivityTypeOrderByCreatedAtDesc(
            ActivityType activityType,
            Pageable pageable
    );

    Page<ActivityLog> findByEntityIdOrderByCreatedAtDesc(
            UUID entityId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "actor")
    Page<ActivityLog> findByActivityTypeInOrderByCreatedAtDesc(
            Collection<ActivityType> activityTypes,
            Pageable pageable
    );
}
