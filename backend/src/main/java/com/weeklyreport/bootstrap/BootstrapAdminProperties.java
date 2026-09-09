package com.weeklyreport.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap-admin")
public record BootstrapAdminProperties(
        boolean enabled,
        String email,
        String password,
        String firstName,
        String lastName
) {
    public BootstrapAdminProperties {
        if (enabled) {
            if (email == null || !email.contains("@")) {
                throw new IllegalArgumentException("BOOTSTRAP_ADMIN_EMAIL must be a valid email address");
            }
            if (password == null || password.length() < 12 || password.length() > 128) {
                throw new IllegalArgumentException("BOOTSTRAP_ADMIN_PASSWORD must contain 12 to 128 characters");
            }
            if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
                throw new IllegalArgumentException("Bootstrap administrator first and last names are required");
            }
        }
    }
}
