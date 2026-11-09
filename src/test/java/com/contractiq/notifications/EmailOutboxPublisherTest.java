package com.contractiq.notifications;

import com.contractiq.shared.AuditService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailOutboxPublisherTest {
    private final EmailOutboxRepository emails = mock(EmailOutboxRepository.class);
    private final BrevoEmailSender sender = mock(BrevoEmailSender.class);
    private final AuditService audit = mock(AuditService.class);
    private final EmailOutboxPublisher publisher = new EmailOutboxPublisher(emails, sender, audit, true);

    @Test
    void marksEmailSentAndAuditsDelivery() {
        EmailOutboxEntity email = email();
        when(emails.findPending(eq(5), any())).thenReturn(List.of(email));

        publisher.sendPending();

        assertNotNull(email.sentAt);
        assertNull(email.lastError);
        verify(sender).send(email.recipient, email.subject, email.htmlContent);
        verify(audit).record(email.organizationId, null, "REMINDER_EMAIL_ATTEMPTED", "NOTIFICATION", email.notificationId);
        verify(audit).record(email.organizationId, null, "REMINDER_EMAIL_SENT", "NOTIFICATION", email.notificationId);
    }

    @Test
    void keepsFailedEmailQueuedAndRecordsOnlyTheExceptionType() {
        EmailOutboxEntity email = email();
        when(emails.findPending(eq(5), any())).thenReturn(List.of(email));
        doThrow(new IllegalStateException("response contained sensitive provider data"))
                .when(sender).send(email.recipient, email.subject, email.htmlContent);

        publisher.sendPending();

        assertEquals(1, email.attemptCount);
        assertEquals("DELIVERY_FAILURE:IllegalStateException", email.lastError);
        assertNull(email.sentAt);
        verify(audit).record(email.organizationId, null, "REMINDER_EMAIL_FAILED", "NOTIFICATION", email.notificationId);
    }

    private EmailOutboxEntity email() {
        EmailOutboxEntity email = new EmailOutboxEntity();
        email.id = "email-1";
        email.organizationId = "org-1";
        email.notificationId = "notification-1";
        email.recipient = "recipient@example.test";
        email.subject = "Reminder";
        email.htmlContent = "<p>Reminder</p>";
        return email;
    }
}
