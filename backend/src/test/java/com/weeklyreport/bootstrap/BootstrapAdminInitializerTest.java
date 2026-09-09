package com.weeklyreport.bootstrap;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BootstrapAdminInitializerTest {

    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final BootstrapAdminProperties properties = new BootstrapAdminProperties(
            true,
            " First.Admin@Example.com ",
            "a-secure-bootstrap-password",
            " First ",
            " Admin "
    );
    private final BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(
            users, passwords, properties
    );

    @Test
    void createsOnlyAdministratorInEmptyDatabaseWithEncodedPassword() {
        when(users.findByEmailIgnoreCase("first.admin@example.com")).thenReturn(Optional.empty());
        when(users.count()).thenReturn(0L);
        when(passwords.encode("a-secure-bootstrap-password")).thenReturn("encoded-password");
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        initializer.run(mock(ApplicationArguments.class));

        var saved = ArgumentCaptor.forClass(User.class);
        verify(users).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("first.admin@example.com");
        assertThat(saved.getValue().getFirstName()).isEqualTo("First");
        assertThat(saved.getValue().getLastName()).isEqualTo("Admin");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("encoded-password");
        assertThat(saved.getValue().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void refusesPrivilegeBootstrapWhenDatabaseAlreadyContainsOtherUsers() {
        when(users.findByEmailIgnoreCase("first.admin@example.com")).thenReturn(Optional.empty());
        when(users.count()).thenReturn(1L);

        assertThatThrownBy(() -> initializer.run(mock(ApplicationArguments.class)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only when the database has no users");
        verify(users, never()).saveAndFlush(any());
    }
}
