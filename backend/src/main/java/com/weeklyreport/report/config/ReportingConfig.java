package com.weeklyreport.report.config;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ReportingProperties.class)
public class ReportingConfig {

    @Bean
    Clock reportingClock(ReportingProperties properties) {
        return Clock.system(properties.timeZone());
    }
}
