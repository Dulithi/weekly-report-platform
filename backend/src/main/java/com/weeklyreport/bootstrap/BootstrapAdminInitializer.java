package com.weeklyreport.bootstrap;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.weeklyreport.user.UserRole;
import com.weeklyreport.user.entity.User;
import com.weeklyreport.user.repository.UserRepository;

@Component
@ConditionalOnProperty(name = "app.bootstrap-admin.enabled", havingValue = "true")
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties properties;

    public BootstrapAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            BootstrapAdminProperties properties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        String email = properties.email().trim().toLowerCase(Locale.ROOT);
        var existing = userRepository.findByEmailIgnoreCase(email);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getRole() != UserRole.ADMIN || !user.isActive()) {
                throw new IllegalStateException(
                        "The bootstrap email belongs to an account that is not an active administrator"
                );
            }
            LOGGER.info("bootstrap_admin outcome=already_exists userId={}", user.getId());
            return;
        }
        if (userRepository.count() != 0) {
            throw new IllegalStateException(
                    "Administrator bootstrap is allowed only when the database has no users"
            );
        }

        User admin = userRepository.saveAndFlush(new User(
                email,
                passwordEncoder.encode(properties.password()),
                properties.firstName().trim(),
                properties.lastName().trim(),
                UserRole.ADMIN
        ));
        LOGGER.info("bootstrap_admin outcome=created userId={}", admin.getId());
    }
}
