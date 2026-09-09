package com.weeklyreport.user.mail;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.invitation-mail")
public record InvitationMailProperties(
        boolean enabled,
        String from,
        String acceptanceBaseUrl,
        String applicationName
) {
    public InvitationMailProperties {
        if (enabled) {
            if (from == null || from.isBlank()) {
                throw new IllegalArgumentException("INVITATION_EMAIL_FROM is required when invitation email is enabled");
            }
            if (applicationName == null || applicationName.isBlank()) {
                throw new IllegalArgumentException("INVITATION_EMAIL_APP_NAME is required when invitation email is enabled");
            }
            URI uri;
            try {
                uri = URI.create(acceptanceBaseUrl);
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("INVITATION_ACCEPTANCE_BASE_URL must be a valid URL", exception);
            }
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("INVITATION_ACCEPTANCE_BASE_URL must be an HTTP(S) origin");
            }
            acceptanceBaseUrl = acceptanceBaseUrl.replaceAll("/+$", "");
        }
    }
}
