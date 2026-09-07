package com.weeklyreport.report.content;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.weeklyreport.project.entity.Project;
import com.weeklyreport.report.TaskPriority;
import com.weeklyreport.report.entity.ReportVersion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "planned_task")
public class PlannedTask {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_version_id", nullable = false)
    private ReportVersion reportVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "task_name", nullable = false, length = 255)
    private String taskName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected PlannedTask() {
    }

    public PlannedTask(
            ReportVersion reportVersion,
            Project project,
            String taskName,
            String description,
            TaskPriority priority,
            Integer estimatedMinutes,
            int sortOrder
    ) {
        this.reportVersion = reportVersion;
        this.project = project;
        this.taskName = taskName;
        this.description = description;
        this.priority = priority;
        this.estimatedMinutes = estimatedMinutes;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public ReportVersion getReportVersion() {
        return reportVersion;
    }

    public Project getProject() {
        return project;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getDescription() {
        return description;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public Integer getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    
}