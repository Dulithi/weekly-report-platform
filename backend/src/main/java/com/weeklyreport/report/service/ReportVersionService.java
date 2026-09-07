package com.weeklyreport.report.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.report.dto.ReportVersionResponse;
import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.report.mapper.ReportMapper;
import com.weeklyreport.report.repository.AchievementRepository;
import com.weeklyreport.report.repository.BlockerRepository;
import com.weeklyreport.report.repository.CompletedTaskRepository;
import com.weeklyreport.report.repository.PlannedTaskRepository;
import com.weeklyreport.report.repository.TimeEntryRepository;

@Service
public class ReportVersionService {

    private final CompletedTaskRepository completedTaskRepository;
    private final PlannedTaskRepository plannedTaskRepository;
    private final BlockerRepository blockerRepository;
    private final AchievementRepository achievementRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final ReportMapper reportMapper;

    public ReportVersionService(
            CompletedTaskRepository completedTaskRepository,
            PlannedTaskRepository plannedTaskRepository,
            BlockerRepository blockerRepository,
            AchievementRepository achievementRepository,
            TimeEntryRepository timeEntryRepository,
            ReportMapper reportMapper
    ) {
        this.completedTaskRepository = completedTaskRepository;
        this.plannedTaskRepository = plannedTaskRepository;
        this.blockerRepository = blockerRepository;
        this.achievementRepository = achievementRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.reportMapper = reportMapper;
    }

    @Transactional(readOnly = true)
    public ReportVersionResponse getResponse(ReportVersion version) {
        var versionId = version.getId();
        return reportMapper.toVersionResponse(
                version,
                completedTaskRepository.findByReportVersionIdOrderBySortOrderAsc(versionId),
                plannedTaskRepository.findByReportVersionIdOrderBySortOrderAsc(versionId),
                blockerRepository.findByReportVersionIdOrderBySortOrderAsc(versionId),
                achievementRepository.findByReportVersionIdOrderBySortOrderAsc(versionId),
                timeEntryRepository.findByReportVersionId(versionId)
        );
    }
}
