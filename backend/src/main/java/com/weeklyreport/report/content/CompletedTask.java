package com.weeklyreport.report.content;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.weeklyreport.project.entity.Project;
import com.weeklyreport.report.TaskPriority;
import com.weeklyreport.report.TaskStatus;
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
@Table(name = "completed_task")
public class CompletedTask {

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

    @Column(name = "planned_percentage", nullable = false)
    private int plannedPercentage;

    @Column(name = "actual_percentage", nullable = false)
    private int actualPercentage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskStatus status;

    @Column(name = "planned_minutes")
    private Integer plannedMinutes;

    @Column(name = "spent_minutes")
    private Integer spentMinutes;

    @Column(columnDefinition = "TEXT")
    private String deliverable;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected CompletedTask() {
    }

    public CompletedTask(
            ReportVersion reportVersion,
            Project project,
            String taskName,
            String description,
            TaskPriority priority,
            int plannedPercentage,
            int actualPercentage,
            TaskStatus status,
            Integer plannedMinutes,
            Integer spentMinutes,
            String deliverable,
            int sortOrder
    ) {
        this.reportVersion = reportVersion;
        this.project = project;
        this.taskName = taskName;
        this.description = description;
        this.priority = priority;
        this.plannedPercentage = plannedPercentage;
        this.actualPercentage = actualPercentage;
        this.status = status;
        this.plannedMinutes = plannedMinutes;
        this.spentMinutes = spentMinutes;
        this.deliverable = deliverable;
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

    public int getPlannedPercentage() {
        return plannedPercentage;
    }

    public int getActualPercentage() {
        return actualPercentage;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Integer getPlannedMinutes() {
        return plannedMinutes;
    }

    public Integer getSpentMinutes() {
        return spentMinutes;
    }

    public String getDeliverable() {
        return deliverable;
    }

    public int getSortOrder() {
        return sortOrder;
    }


}