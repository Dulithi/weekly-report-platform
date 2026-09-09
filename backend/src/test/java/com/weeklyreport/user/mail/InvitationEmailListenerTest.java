package com.weeklyreport.user.mail;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import com.weeklyreport.user.UserRole;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InvitationEmailListenerTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final InvitationMailProperties properties = new InvitationMailProperties(
            true,
            "reports@example.com",
            "https://reports.example.com/",
            "Weekly Reports"
    );
    private final InvitationEmailListener listener = new InvitationEmailListener(mailSender, properties);

    @Test
    void sendsSingleUseTokenInUrlFragment() {
        var event = event();

        listener.send(event);

        var message = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertThat(message.getValue().getTo()).containsExactly("member@example.com");
        assertThat(message.getValue().getFrom()).isEqualTo("reports@example.com");
        assertThat(message.getValue().getText())
                .contains("https://reports.example.com/accept-invitation#token=single-use-token")
                .contains("TEAM MEMBER")
                .contains("2030-01-01T00:00:00Z");
    }

    @Test
    void mailFailureDoesNotInvalidateCommittedInvitation() {
        doThrow(new MailSendException("provider unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> listener.send(event())).doesNotThrowAnyException();
    }

    private UserInvitationCreatedEvent event() {
        return new UserInvitationCreatedEvent(
                UUID.randomUUID(),
                "member@example.com",
                UserRole.TEAM_MEMBER,
                Instant.parse("2030-01-01T00:00:00Z"),
                "single-use-token"
        );
    }
}
