package com.weeklyreport.project.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import com.weeklyreport.project.ProjectStatus;
import com.weeklyreport.project.entity.Project;

public interface ProjectRepository
        extends JpaRepository<Project, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select project from Project project where project.id = :id")
    java.util.Optional<Project> findByIdForUpdate(@Param("id") UUID id);

    boolean existsByNameIgnoreCaseAndStatus(
            String name,
            ProjectStatus status
    );

    boolean existsByNameIgnoreCaseAndStatusAndIdNot(
            String name,
            ProjectStatus status,
            UUID id
     );

    Page<Project> findByStatus(
            ProjectStatus status,
            Pageable pageable
    );

    @Query("""
            select project
            from Project project
            where (:status is null or project.status = :status)
              and exists (
                select 1
                from ProjectMember membership
                where membership.project = project
                  and membership.user.id = :userId
              )
            """)
    Page<Project> findAssignedToUser(
            @Param("userId") UUID userId,
            @Param("status") ProjectStatus status,
            Pageable pageable
    );
}
