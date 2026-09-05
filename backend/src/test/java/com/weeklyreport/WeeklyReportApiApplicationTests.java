package com.weeklyreport;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.weeklyreport.support.PostgresTestContainerConfiguration;

@SpringBootTest
@Import(PostgresTestContainerConfiguration.class)
class WeeklyReportApiApplicationTests {

    @Test
    void contextLoads() {
    }
}
