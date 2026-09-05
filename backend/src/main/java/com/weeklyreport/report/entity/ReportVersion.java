package com.weeklyreport.report.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "report_version",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_report_version_number",
                        columnNames = {
                                "report_id",
                                "version_number"
                        }
                )
        }
)
public class ReportVersion {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false)
    private WeeklyReport report;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    protected ReportVersion() {
    }

    public ReportVersion(
            WeeklyReport report,
            int versionNumber,
            String notes
    ) {
        this.report = report;
        this.versionNumber = versionNumber;
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public WeeklyReport getReport() {
        return report;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}