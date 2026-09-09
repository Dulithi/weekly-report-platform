package com.weeklyreport.user.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnProperty(name = "app.invitation-mail.enabled", havingValue = "true")
public class InvitationEmailListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvitationEmailListener.class);

    private final JavaMailSender mailSender;
    private final InvitationMailProperties properties;

    public InvitationEmailListener(JavaMailSender mailSender, InvitationMailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void send(UserInvitationCreatedEvent event) {
        String acceptanceUrl = properties.acceptanceBaseUrl()
                + "/accept-invitation#token=" + event.acceptanceToken();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(event.recipient());
        message.setSubject("You are invited to " + properties.applicationName());
        message.setText("""
                You have been invited to join %s as %s.

                Accept your invitation:
                %s

                This single-use link expires at %s. If you were not expecting this invitation, ignore this email.
                """.formatted(
                properties.applicationName(),
                event.role().name().replace('_', ' '),
                acceptanceUrl,
                event.expiresAt()
        ));
        try {
            mailSender.send(message);
            LOGGER.info("invitation_email outcome=sent invitationId={}", event.invitationId());
        } catch (MailException exception) {
            // The invitation is already committed and its one-time link remains available to the admin.
            // Never include the token, recipient or provider response in logs.
            LOGGER.error(
                    "invitation_email outcome=failed invitationId={} errorType={}",
                    event.invitationId(), exception.getClass().getSimpleName()
            );
        }
    }
}
