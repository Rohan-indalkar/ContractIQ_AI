package com.contractiq.notifications;

import com.contractiq.shared.AuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class EmailOutboxPublisher {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(EmailOutboxPublisher.class);
    private final EmailOutboxRepository emails;
    private final BrevoEmailSender sender;
    private final AuditService audit;
    private final boolean enabled;

    public EmailOutboxPublisher(EmailOutboxRepository emails, BrevoEmailSender sender, AuditService audit,
                                @Value("${app.email.brevo.enabled:false}") boolean enabled) {
        this.emails = emails;
        this.sender = sender;
        this.audit = audit;
        this.enabled = enabled;
    }

    @jakarta.annotation.PostConstruct
    void validateConfiguration() {
        if (enabled) sender.validateConfiguration();
    }

    @Scheduled(fixedDelayString = "${app.email.brevo.poll-interval:5000}")
    @Transactional
    public void sendPending() {
        if (!enabled) return;
        for (EmailOutboxEntity email : emails.findPending(5, org.springframework.data.domain.PageRequest.of(0, 10))) {
            try {
                audit.record(email.organizationId, null, "REMINDER_EMAIL_ATTEMPTED", "NOTIFICATION", email.notificationId);
                sender.send(email.recipient, email.subject, email.htmlContent);
                email.sentAt = LocalDateTime.now();
                email.lastError = null;
                emails.save(email);
                audit.record(email.organizationId, null, "REMINDER_EMAIL_SENT", "NOTIFICATION", email.notificationId);
            } catch (Exception exception) {
                email.attemptCount++;
                email.lastError = "DELIVERY_FAILURE:" + exception.getClass().getSimpleName();
                emails.save(email);
                audit.record(email.organizationId, null, "REMINDER_EMAIL_FAILED", "NOTIFICATION", email.notificationId);
                LOGGER.warn("Brevo reminder delivery failed emailOutboxId={} attempts={} exceptionType={}",
                        email.id, email.attemptCount, exception.getClass().getSimpleName());
            }
        }
    }
}
