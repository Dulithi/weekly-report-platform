package com.weeklyreport.project.entity;

import com.weeklyreport.common.entity.BaseEntity;
import com.weeklyreport.project.ProjectStatus;
import com.weeklyreport.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "project")
public class Project extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    protected Project() {
    }

    public Project(
            String name,
            String description,
            User createdBy
    ) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void update(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public void archive() {
        this.status = ProjectStatus.ARCHIVED;
    }

    public void activate() {
        this.status = ProjectStatus.ACTIVE;
    }
}