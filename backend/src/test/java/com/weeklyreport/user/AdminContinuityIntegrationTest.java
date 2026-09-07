package com.weeklyreport.user;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.weeklyreport.common.exception.ConflictException;
import com.weeklyreport.support.PostgresIntegrationTest;
import com.weeklyreport.user.dto.UpdateRoleRequest;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;
import com.weeklyreport.user.service.UserService;

@SpringBootTest
// No surrounding test transaction: concurrent service calls need independent commits.
class AdminContinuityIntegrationTest extends PostgresIntegrationTest {

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;

    private final List<UUID> createdIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (UUID id : createdIds) {
            jdbcTemplate.update("delete from activity_log where actor_user_id = ? or entity_id = ?", id, id);
        }
        for (UUID id : createdIds) {
            jdbcTemplate.update("delete from app_user where id = ?", id);
        }
    }

    @Test
    void onlyActiveAdminCannotDemoteThemselves() {
        User admin = createAdmin(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> userService.changeUserRole(
                        admin.getId(), new UpdateRoleRequest(UserRole.TEAM_MEMBER), admin.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("At least one active administrator is required");

        assertThat(reload(admin).getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void inactiveAdminDoesNotSatisfyTheContinuityRule() {
        User active = createAdmin(true);
        createAdmin(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> userService.changeUserRole(
                        active.getId(), new UpdateRoleRequest(UserRole.MANAGER), active.getId()))
                .isInstanceOf(ConflictException.class);

        assertThat(activeAdminCount()).isEqualTo(1);
    }

    @Test
    void oneOfTwoActiveAdminsCanBeDemoted() {
        User first = createAdmin(true);
        User second = createAdmin(true);

        userService.changeUserRole(first.getId(), new UpdateRoleRequest(UserRole.MANAGER), second.getId());

        assertThat(reload(first).getRole()).isEqualTo(UserRole.MANAGER);
        assertThat(activeAdminCount()).isEqualTo(1);
    }

    @Test
    void concurrentDemotionsCannotRemoveBothActiveAdmins() throws Exception {
        User first = createAdmin(true);
        User second = createAdmin(true);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstResult = executor.submit(() -> demoteAfter(start, first));
            var secondResult = executor.submit(() -> demoteAfter(start, second));
            start.countDown();

            List<String> results = List.of(
                    firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS));
            assertThat(results).containsExactlyInAnyOrder("changed", "blocked");
        }

        assertThat(activeAdminCount()).isEqualTo(1);
    }

    @Test
    void onlyActiveAdminCannotBeDeactivatedThroughTheService() {
        User admin = createAdmin(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        userService.deactivateUser(admin.getId(), UUID.randomUUID()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("At least one active administrator is required");

        assertThat(reload(admin).isActive()).isTrue();
    }

    @Test
    void concurrentDeactivationsCannotRemoveBothActiveAdmins() throws Exception {
        User first = createAdmin(true);
        User second = createAdmin(true);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstResult = executor.submit(() -> deactivateAfter(start, first, second));
            var secondResult = executor.submit(() -> deactivateAfter(start, second, first));
            start.countDown();

            List<String> results = List.of(
                    firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS));
            assertThat(results).containsExactlyInAnyOrder("changed", "blocked");
        }

        assertThat(activeAdminCount()).isEqualTo(1);
    }

    private String demoteAfter(CountDownLatch start, User admin) throws Exception {
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Admin update start timed out");
        }
        try {
            userService.changeUserRole(admin.getId(),
                    new UpdateRoleRequest(UserRole.TEAM_MEMBER), admin.getId());
            return "changed";
        } catch (ConflictException exception) {
            return "blocked";
        }
    }

    private String deactivateAfter(CountDownLatch start, User target, User actor) throws Exception {
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Admin update start timed out");
        }
        try {
            userService.deactivateUser(target.getId(), actor.getId());
            return "changed";
        } catch (ConflictException exception) {
            return "blocked";
        }
    }

    private User createAdmin(boolean active) {
        User user = new User("admin-continuity-" + UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("VerySecurePassword123!"), "Admin", "Test", UserRole.ADMIN);
        if (!active) {
            user.deactivate();
        }
        user = userRepository.saveAndFlush(user);
        createdIds.add(user.getId());
        return user;
    }

    private User reload(User user) {
        return userRepository.findById(user.getId()).orElseThrow();
    }

    private int activeAdminCount() {
        return jdbcTemplate.queryForObject(
                "select count(*) from app_user where role = 'ADMIN' and active", Integer.class);
    }
}
