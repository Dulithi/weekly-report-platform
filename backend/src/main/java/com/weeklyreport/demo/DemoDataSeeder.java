package com.weeklyreport.demo;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import com.weeklyreport.project.dto.AssignProjectMemberRequest;
import com.weeklyreport.project.dto.CreateProjectRequest;
import com.weeklyreport.project.service.ProjectMemberService;
import com.weeklyreport.project.service.ProjectService;
import com.weeklyreport.report.TaskPriority;
import com.weeklyreport.report.TaskStatus;
import com.weeklyreport.report.TaskType;
import com.weeklyreport.report.dto.AchievementRequest;
import com.weeklyreport.report.dto.BlockerRequest;
import com.weeklyreport.report.dto.CompletedTaskRequest;
import com.weeklyreport.report.dto.CreateWeeklyReportRequest;
import com.weeklyreport.report.dto.PlannedTaskRequest;
import com.weeklyreport.report.dto.TimeEntryRequest;
import com.weeklyreport.report.dto.UpdateWeeklyReportRequest;
import com.weeklyreport.report.dto.WeeklyReportResponse;
import com.weeklyreport.report.entity.WeeklyReport;
import com.weeklyreport.report.repository.WeeklyReportRepository;
import com.weeklyreport.report.service.WeeklyReportService;
import com.weeklyreport.review.ReviewAction;
import com.weeklyreport.review.service.ReviewService;
import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.dto.AssignManagerRequest;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;
import com.weeklyreport.user.service.UserManagementService;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.demo-seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    public static final String ADMIN_EMAIL = "demo.admin@weekly.local";
    public static final String MANAGER_EMAIL = "demo.manager@weekly.local";

    private final UserRepository userRepository;
    private final WeeklyReportRepository weeklyReportRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProjectService projectService;
    private final ProjectMemberService projectMemberService;
    private final UserManagementService userManagementService;
    private final WeeklyReportService weeklyReportService;
    private final ReviewService reviewService;
    private final Clock clock;
    private final EntityManager entityManager;
    private final String demoPassword;

    public DemoDataSeeder(
            UserRepository userRepository,
            WeeklyReportRepository weeklyReportRepository,
            PasswordEncoder passwordEncoder,
            ProjectService projectService,
            ProjectMemberService projectMemberService,
            UserManagementService userManagementService,
            WeeklyReportService weeklyReportService,
            ReviewService reviewService,
            Clock clock,
            EntityManager entityManager,
            @Value("${app.demo-seed.password:}") String demoPassword
    ) {
        this.userRepository = userRepository;
        this.weeklyReportRepository = weeklyReportRepository;
        this.passwordEncoder = passwordEncoder;
        this.projectService = projectService;
        this.projectMemberService = projectMemberService;
        this.userManagementService = userManagementService;
        this.weeklyReportService = weeklyReportService;
        this.reviewService = reviewService;
        this.clock = clock;
        this.entityManager = entityManager;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (userRepository.existsByEmailIgnoreCase(ADMIN_EMAIL)) {
            return;
        }
        if (demoPassword.length() < 12) {
            throw new IllegalStateException(
                    "DEMO_PASSWORD must contain at least 12 characters when demo seeding is enabled"
            );
        }

        User admin = createUser(ADMIN_EMAIL, "Demo", "Admin", UserRole.ADMIN);
        User manager = createUser(MANAGER_EMAIL, "Maya", "Manager", UserRole.MANAGER);
        List<User> members = List.of(
                createUser("alex.morgan@weekly.local", "Alex", "Morgan", UserRole.TEAM_MEMBER),
                createUser("priya.shah@weekly.local", "Priya", "Shah", UserRole.TEAM_MEMBER),
                createUser("sam.perera@weekly.local", "Sam", "Perera", UserRole.TEAM_MEMBER),
                createUser("mei.chen@weekly.local", "Mei", "Chen", UserRole.TEAM_MEMBER),
                createUser("jordan.silva@weekly.local", "Jordan", "Silva", UserRole.TEAM_MEMBER)
        );

        members.forEach(member -> userManagementService.assignManager(
                member.getId(), new AssignManagerRequest(manager.getId()), admin.getId()
        ));

        List<UUID> projects = List.of(
                createProject("Client Portal", "Customer-facing account portal", admin),
                createProject("Internal Tooling", "Engineering productivity tools", admin),
                createProject("Research and Development", "Product experiments", admin),
                createProject("Operations", "Internal operations and support", admin)
        );
        for (int index = 0; index < members.size(); index++) {
            assignProject(projects.get(index % projects.size()), members.get(index), admin);
            assignProject(projects.get((index + 1) % projects.size()), members.get(index), admin);
        }

        LocalDate currentMonday = LocalDate.now(clock)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        seedReport(members.get(0), manager, projects.get(0), currentMonday,
                SeedOutcome.CORRECTED_AND_APPROVED, 1);
        seedReport(members.get(1), manager, projects.get(1), currentMonday,
                SeedOutcome.SUBMITTED, 2);
        seedReport(members.get(2), manager, projects.get(2), currentMonday,
                SeedOutcome.NEEDS_CORRECTION, 3);
        seedReport(members.get(3), manager, projects.get(3), currentMonday,
                SeedOutcome.DRAFT, 4);

        for (int weeksAgo = 1; weeksAgo <= 3; weeksAgo++) {
            LocalDate week = currentMonday.minusWeeks(weeksAgo);
            for (int memberIndex = 0; memberIndex < members.size(); memberIndex++) {
                SeedOutcome outcome = historicalOutcome(weeksAgo, memberIndex);
                seedReport(
                        members.get(memberIndex),
                        manager,
                        projects.get((memberIndex + weeksAgo) % projects.size()),
                        week,
                        outcome,
                        weeksAgo * 10 + memberIndex
                );
            }
        }
    }

    private User createUser(String email, String firstName, String lastName, UserRole role) {
        return userRepository.save(new User(
                email,
                passwordEncoder.encode(demoPassword),
                firstName,
                lastName,
                role
        ));
    }

    private UUID createProject(String name, String description, User admin) {
        return projectService.createProject(
                new CreateProjectRequest(name, description), admin.getId()
        ).id();
    }

    private void assignProject(UUID projectId, User member, User admin) {
        projectMemberService.assignProjectMember(
                projectId, new AssignProjectMemberRequest(member.getId()), admin.getId()
        );
    }

    private void seedReport(
            User member,
            User manager,
            UUID projectId,
            LocalDate weekStart,
            SeedOutcome outcome,
            int variant
    ) {
        WeeklyReportResponse created = weeklyReportService.create(
                member.getId(), new CreateWeeklyReportRequest(weekStart)
        );
        weeklyReportService.update(
                created.id(),
                member.getId(),
                reportContent(projectId, member.getFirstName(), variant, false, created.currentVersion().entityVersion())
        );
        persistenceBoundary();

        if (outcome == SeedOutcome.DRAFT) {
            return;
        }

        weeklyReportService.submit(created.id(), member.getId());
        persistenceBoundary();
        if (outcome == SeedOutcome.SUBMITTED) {
            return;
        }
        if (outcome == SeedOutcome.APPROVED) {
            reviewService.createReview(created.id(), manager.getId(), ReviewAction.APPROVED, "Looks good");
            persistenceBoundary();
            return;
        }

        reviewService.createReview(
                created.id(), manager.getId(), ReviewAction.CHANGES_REQUESTED,
                "Please clarify the deliverable and resolution plan."
        );
        persistenceBoundary();
        if (outcome == SeedOutcome.NEEDS_CORRECTION) {
            return;
        }

        WeeklyReport correction = weeklyReportRepository.findById(created.id()).orElseThrow();
        weeklyReportService.update(
                created.id(),
                member.getId(),
                reportContent(
                        projectId,
                        member.getFirstName(),
                        variant,
                        true,
                        correction.getCurrentVersion().getEntityVersion()
                )
        );
        persistenceBoundary();
        weeklyReportService.submit(created.id(), member.getId());
        persistenceBoundary();
        reviewService.createReview(
                created.id(), manager.getId(), ReviewAction.APPROVED,
                "Correction verified and approved."
        );
        persistenceBoundary();
    }

    private void persistenceBoundary() {
        entityManager.flush();
        entityManager.clear();
    }

    private UpdateWeeklyReportRequest reportContent(
            UUID projectId,
            String firstName,
            int variant,
            boolean corrected,
            long entityVersion
    ) {
        boolean unresolvedBlocker = variant % 3 == 0 && !corrected;
        int actualPercentage = corrected ? 100 : 70 + variant % 4 * 10;
        TaskStatus taskStatus = actualPercentage == 100
                ? TaskStatus.COMPLETED
                : TaskStatus.IN_PROGRESS;

        return new UpdateWeeklyReportRequest(
                corrected
                        ? "Updated after manager feedback with links and verification notes."
                        : "Weekly report for " + firstName + " with supporting links.",
                List.of(new CompletedTaskRequest(
                        projectId,
                        "Deliver iteration " + variant,
                        "Implemented and reviewed the planned scope.",
                        variant % 2 == 0 ? TaskPriority.HIGH : TaskPriority.MEDIUM,
                        100,
                        actualPercentage,
                        taskStatus,
                        480,
                        420 + variant * 5,
                        "https://example.com/demo/deliverables/" + variant
                )),
                List.of(new PlannedTaskRequest(
                        projectId,
                        "Plan iteration " + (variant + 1),
                        "Prepare the next implementation milestone.",
                        TaskPriority.MEDIUM,
                        360
                )),
                List.of(new BlockerRequest(
                        unresolvedBlocker
                                ? "Waiting for external API access"
                                : "API access confirmed",
                        true,
                        !unresolvedBlocker
                )),
                List.of(new AchievementRequest(
                        corrected
                                ? "Addressed review feedback and verified the result"
                                : "Completed the main weekly milestone",
                        true
                )),
                List.of(
                        new TimeEntryRequest(TaskType.DEVELOPMENT, 1_560 + variant * 10),
                        new TimeEntryRequest(TaskType.TESTING, 360 + variant * 5),
                        new TimeEntryRequest(TaskType.MEETINGS, 240),
                        new TimeEntryRequest(TaskType.DOCUMENTATION, 180)
                ),
                entityVersion
        );
    }

    private SeedOutcome historicalOutcome(int weeksAgo, int memberIndex) {
        if (weeksAgo == 1 && memberIndex == 4) {
            return SeedOutcome.NEEDS_CORRECTION;
        }
        if (weeksAgo == 2 && memberIndex == 1) {
            return SeedOutcome.CORRECTED_AND_APPROVED;
        }
        if (weeksAgo == 3 && memberIndex == 3) {
            return SeedOutcome.SUBMITTED;
        }
        return SeedOutcome.APPROVED;
    }

    private enum SeedOutcome {
        DRAFT,
        SUBMITTED,
        NEEDS_CORRECTION,
        APPROVED,
        CORRECTED_AND_APPROVED
    }
}
