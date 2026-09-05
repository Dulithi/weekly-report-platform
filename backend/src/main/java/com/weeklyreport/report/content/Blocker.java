package com.weeklyreport.report.content;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import com.weeklyreport.report.entity.ReportVersion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "blocker")
public class Blocker {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_version_id", nullable = false)
    private ReportVersion reportVersion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_key_blocker", nullable = false)
    private boolean keyBlocker;

    @Column(nullable = false)
    private boolean resolved;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Blocker() {
    }

    public Blocker(
            ReportVersion reportVersion,
            String description,
            boolean keyBlocker,
            boolean resolved,
            int sortOrder
    ) {
        this.reportVersion = reportVersion;
        this.description = description;
        this.keyBlocker = keyBlocker;
        this.resolved = resolved;
        this.sortOrder = sortOrder;
    }

    public UUID getId() {
        return id;
    }

    public ReportVersion getReportVersion() {
        return reportVersion;
    }

    public String getDescription() {
        return description;
    }

    public boolean isKeyBlocker() {
        return keyBlocker;
    }

    public boolean isResolved() {
        return resolved;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}