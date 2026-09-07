package com.weeklyreport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WeeklyReportApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(WeeklyReportApiApplication.class, args);
	}

}
