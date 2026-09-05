package com.weeklyreport.report.content;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.weeklyreport.report.TaskType;
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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "time_entry",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_time_entry_type_per_version",
                        columnNames = {
                                "report_version_id",
                                "task_type"
                        }
                )
        }
)
public class TimeEntry {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_version_id", nullable = false)
    private ReportVersion reportVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 30)
    private TaskType taskType;

    @Column(nullable = false)
    private int minutes;

    protected TimeEntry() {
    }

    public TimeEntry(
            ReportVersion reportVersion,
            TaskType taskType,
            int minutes
    ) {
        this.reportVersion = reportVersion;
        this.taskType = taskType;
        this.minutes = minutes;
    }

    public UUID getId() {
        return id;
    }

    public ReportVersion getReportVersion() {
        return reportVersion;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public int getMinutes() {
        return minutes;
    }
}