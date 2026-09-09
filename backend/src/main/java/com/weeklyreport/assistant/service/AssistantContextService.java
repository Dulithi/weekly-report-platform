package com.weeklyreport.assistant.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.assistant.config.AssistantProperties;
import com.weeklyreport.report.content.Achievement;
import com.weeklyreport.report.content.Blocker;
import com.weeklyreport.report.content.CompletedTask;
import com.weeklyreport.report.content.PlannedTask;
import com.weeklyreport.report.content.TimeEntry;
import com.weeklyreport.report.entity.ReportVersion;
import com.weeklyreport.report.repository.AchievementRepository;
import com.weeklyreport.report.repository.BlockerRepository;
import com.weeklyreport.report.repository.CompletedTaskRepository;
import com.weeklyreport.report.repository.PlannedTaskRepository;
import com.weeklyreport.report.repository.ReportVersionRepository;
import com.weeklyreport.report.repository.TimeEntryRepository;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.service.ManagerScopeService;

import tools.jackson.databind.ObjectMapper;

@Service
public class AssistantContextService {
    private static final int MAX_ENTRIES_PER_SECTION = 10;
    private static final int MAX_TEXT_FIELD_CHARACTERS = 500;

    private final ReportVersionRepository reportVersionRepository;
    private final CompletedTaskRepository completedTaskRepository;
    private final PlannedTaskRepository plannedTaskRepository;
    private final BlockerRepository blockerRepository;
    private final AchievementRepository achievementRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final AssistantProperties properties;
    private final ObjectMapper objectMapper;
    private final ManagerScopeService managerScopeService;

    public AssistantContextService(
            ReportVersionRepository reportVersionRepository,
            CompletedTaskRepository completedTaskRepository,
            PlannedTaskRepository plannedTaskRepository,
            BlockerRepository blockerRepository,
            AchievementRepository achievementRepository,
            TimeEntryRepository timeEntryRepository,
            AssistantProperties properties,
            ObjectMapper objectMapper,
            ManagerScopeService managerScopeService
    ) {
        this.reportVersionRepository = reportVersionRepository;
        this.completedTaskRepository = completedTaskRepository;
        this.plannedTaskRepository = plannedTaskRepository;
        this.blockerRepository = blockerRepository;
        this.achievementRepository = achievementRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.managerScopeService = managerScopeService;
    }

    @Transactional(readOnly = true)
    public AssistantContext build(UUID actorId, LocalDate fromWeek, LocalDate throughWeek) {
        List<ReportVersion> versions = reportVersionRepository
                .findLatestSubmittedForActiveMembersBetween(
                        fromWeek,
                        throughWeek,
                        UserRole.TEAM_MEMBER,
                        managerScopeService.visibleMemberIds(actorId),
                        PageRequest.of(0, properties.maxReports())
                );
        if (versions.isEmpty()) {
            return new AssistantContext(fromWeek, throughWeek, "", List.of());
        }

        List<UUID> versionIds = versions.stream().map(ReportVersion::getId).toList();
        Map<UUID, List<CompletedTask>> completed = group(
                completedTaskRepository.findForVersions(versionIds),
                task -> task.getReportVersion().getId()
        );
        Map<UUID, List<PlannedTask>> planned = group(
                plannedTaskRepository.findForVersions(versionIds),
                task -> task.getReportVersion().getId()
        );
        Map<UUID, List<Blocker>> blockers = group(
                blockerRepository.findForVersions(versionIds),
                blocker -> blocker.getReportVersion().getId()
        );
        Map<UUID, List<Achievement>> achievements = group(
                achievementRepository.findForVersions(versionIds),
                achievement -> achievement.getReportVersion().getId()
        );
        Map<UUID, List<TimeEntry>> timeEntries = group(
                timeEntryRepository.findForVersions(versionIds),
                entry -> entry.getReportVersion().getId()
        );

        List<Map<String, Object>> includedReports = new ArrayList<>();
        List<AssistantSource> sources = new ArrayList<>();
        String contextJson = "";
        for (ReportVersion version : versions) {
            String sourceKey = "R" + (sources.size() + 1);
            Map<String, Object> report = reportMap(
                    sourceKey,
                    version,
                    completed.getOrDefault(version.getId(), List.of()),
                    planned.getOrDefault(version.getId(), List.of()),
                    blockers.getOrDefault(version.getId(), List.of()),
                    achievements.getOrDefault(version.getId(), List.of()),
                    timeEntries.getOrDefault(version.getId(), List.of())
            );
            includedReports.add(report);
            String candidateJson = serialize(root(fromWeek, throughWeek, includedReports));
            if (candidateJson.length() > properties.maxContextCharacters()) {
                includedReports.removeLast();
                break;
            }
            contextJson = candidateJson;
            var member = version.getReport().getUser();
            sources.add(new AssistantSource(
                    sourceKey,
                    version.getReport().getId(),
                    version.getVersionNumber(),
                    member.getFirstName() + " " + member.getLastName(),
                    version.getReport().getWeekStart()
            ));
        }

        return new AssistantContext(
                fromWeek,
                throughWeek,
                contextJson,
                List.copyOf(sources)
        );
    }

    private Map<String, Object> root(
            LocalDate fromWeek,
            LocalDate throughWeek,
            List<Map<String, Object>> reports
    ) {
        Map<String, Integer> minutesByMember = new LinkedHashMap<>();
        Map<String, Integer> minutesByProject = new LinkedHashMap<>();
        Map<String, Integer> minutesByTaskType = new LinkedHashMap<>();
        for (Map<String, Object> report : reports) {
            String member = (String) report.get("member");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> entries = (List<Map<String, Object>>) report.get("timeEntries");
            for (Map<String, Object> entry : entries) {
                int minutes = (Integer) entry.get("minutes");
                minutesByMember.merge(member, minutes, Integer::sum);
                minutesByTaskType.merge((String) entry.get("taskType"), minutes, Integer::sum);
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> tasks = (List<Map<String, Object>>) report.get("completedTasks");
            for (Map<String, Object> task : tasks) {
                Integer minutes = (Integer) task.get("spentMinutes");
                if (minutes != null) {
                    minutesByProject.merge((String) task.get("project"), minutes, Integer::sum);
                }
            }
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("scope", Map.of("fromWeek", fromWeek, "throughWeek", throughWeek));
        root.put("serverComputedMinutesByMember", minutesByMember);
        root.put("serverComputedMinutesByProject", minutesByProject);
        root.put("serverComputedMinutesByTaskType", minutesByTaskType);
        root.put("reports", reports);
        return root;
    }

    private Map<String, Object> reportMap(
            String sourceKey,
            ReportVersion version,
            List<CompletedTask> completed,
            List<PlannedTask> planned,
            List<Blocker> blockers,
            List<Achievement> achievements,
            List<TimeEntry> timeEntries
    ) {
        var report = version.getReport();
        var member = report.getUser();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sourceKey", sourceKey);
        result.put("member", member.getFirstName() + " " + member.getLastName());
        result.put("weekStart", report.getWeekStart());
        result.put("weekEnd", report.getWeekEnd());
        result.put("workflowStatus", report.getStatus());
        result.put("submittedVersion", version.getVersionNumber());
        result.put("notes", clip(version.getNotes()));
        result.put("completedTasks", completed.stream().limit(MAX_ENTRIES_PER_SECTION)
                .map(this::completedTaskMap).toList());
        result.put("plannedTasks", planned.stream().limit(MAX_ENTRIES_PER_SECTION)
                .map(this::plannedTaskMap).toList());
        result.put("blockers", blockers.stream().limit(MAX_ENTRIES_PER_SECTION)
                .map(this::blockerMap).toList());
        result.put("achievements", achievements.stream().limit(MAX_ENTRIES_PER_SECTION)
                .map(this::achievementMap).toList());
        result.put("timeEntries", timeEntries.stream().limit(MAX_ENTRIES_PER_SECTION)
                .map(this::timeEntryMap).toList());
        return result;
    }

    private Map<String, Object> completedTaskMap(CompletedTask task) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", clip(task.getTaskName()));
        result.put("description", clip(task.getDescription()));
        result.put("project", task.getProject() == null ? "Uncategorized" : task.getProject().getName());
        result.put("priority", task.getPriority());
        result.put("status", task.getStatus());
        result.put("plannedPercentage", task.getPlannedPercentage());
        result.put("actualPercentage", task.getActualPercentage());
        result.put("plannedMinutes", task.getPlannedMinutes());
        result.put("spentMinutes", task.getSpentMinutes());
        result.put("deliverable", clip(task.getDeliverable()));
        return result;
    }

    private Map<String, Object> plannedTaskMap(PlannedTask task) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("name", clip(task.getTaskName()));
        result.put("description", clip(task.getDescription()));
        result.put("project", task.getProject() == null ? "Uncategorized" : task.getProject().getName());
        result.put("priority", task.getPriority());
        result.put("estimatedMinutes", task.getEstimatedMinutes());
        return result;
    }

    private Map<String, Object> blockerMap(Blocker blocker) {
        return Map.of(
                "description", clip(blocker.getDescription()),
                "key", blocker.isKeyBlocker(),
                "resolved", blocker.isResolved()
        );
    }

    private Map<String, Object> achievementMap(Achievement achievement) {
        return Map.of(
                "description", clip(achievement.getDescription()),
                "key", achievement.isKeyAchievement()
        );
    }

    private Map<String, Object> timeEntryMap(TimeEntry entry) {
        return Map.of("taskType", entry.getTaskType().name(), "minutes", entry.getMinutes());
    }

    private String serialize(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not build assistant context", exception);
        }
    }

    private String clip(String value) {
        if (value == null || value.length() <= MAX_TEXT_FIELD_CHARACTERS) return value;
        return value.substring(0, MAX_TEXT_FIELD_CHARACTERS) + "…";
    }

    private <T> Map<UUID, List<T>> group(List<T> values, Function<T, UUID> versionId) {
        return values.stream().collect(Collectors.groupingBy(
                versionId,
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }
}
