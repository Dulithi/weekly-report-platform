package com.weeklyreport.report.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app.reporting")
public record ReportingProperties(
        @NotNull ZoneId timeZone
) {
}
