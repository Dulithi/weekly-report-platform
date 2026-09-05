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
@Table(name = "achievement")
public class Achievement {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_version_id", nullable = false)
    private ReportVersion reportVersion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_key_achievement", nullable = false)
    private boolean keyAchievement;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Achievement() {
    }

    public Achievement(
            ReportVersion reportVersion,
            String description,
            boolean keyAchievement,
            int sortOrder
    ) {
        this.reportVersion = reportVersion;
        this.description = description;
        this.keyAchievement = keyAchievement;
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

    public boolean isKeyAchievement() {
        return keyAchievement;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}