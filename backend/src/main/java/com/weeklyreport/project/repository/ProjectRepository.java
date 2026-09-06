package com.weeklyreport.project.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.weeklyreport.project.ProjectStatus;
import com.weeklyreport.project.entity.Project;

public interface ProjectRepository
        extends JpaRepository<Project, UUID> {

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
}