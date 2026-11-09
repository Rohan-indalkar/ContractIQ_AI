package com.contractiq.notifications;

import com.contractiq.identity.MembershipRepository;
import com.contractiq.identity.UserRepository;
import com.contractiq.obligations.ObligationEntity;
import com.contractiq.obligations.ObligationRepository;
import com.contractiq.identity.UserEntity;
import com.contractiq.shared.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Component
public class ObligationReminderScheduler {
    private final ObligationRepository obligations;
    private final MembershipRepository memberships;
    private final NotificationRepository notifications;
    private final EmailOutboxRepository emailOutbox;
    private final UserRepository users;
    private final AuditService audit;
    private final ObjectMapper objectMapper;
    private final boolean emailEnabled;

    public ObligationReminderScheduler(ObligationRepository obligations, MembershipRepository memberships,
                                       NotificationRepository notifications, EmailOutboxRepository emailOutbox,
                                       UserRepository users, AuditService audit, ObjectMapper objectMapper,
                                       @Value("${app.email.brevo.enabled:false}") boolean emailEnabled) {
        this.obligations = obligations;
        this.memberships = memberships;
        this.notifications = notifications;
        this.emailOutbox = emailOutbox;
        this.users = users;
        this.audit = audit;
        this.objectMapper = objectMapper;
        this.emailEnabled = emailEnabled;
    }

    @Scheduled(cron = "${app.reminders.cron:0 0 8 * * *}")
    @Transactional
    public void createDueReminders() {
        createForOffset(7);
        createForOffset(1);
    }

    private void createForOffset(int days) {
        for (ObligationEntity obligation : obligations.findAllByDueDateAndStatus(LocalDate.now().plusDays(days), "OPEN")) {
            var members = memberships.findAllByOrganizationId(obligation.organizationId);
            var memberUsers = users.findAllByIdIn(members.stream().map(member -> member.userId).toList()).stream()
                    .collect(java.util.stream.Collectors.toMap(user -> user.id, user -> user));
            for (var member : members) {
                if (notifications.existsByUserIdAndObligationIdAndReminderOffset(member.userId, obligation.id, days)) continue;
                NotificationEntity notification = new NotificationEntity();
                notification.id = UUID.randomUUID().toString();
                notification.organizationId = obligation.organizationId;
                notification.userId = member.userId;
                notification.obligationId = obligation.id;
                notification.reminderOffset = days;
                notification.kind = "OBLIGATION_DUE";
                notification.payloadJson = objectMapper.valueToTree(Map.of("obligationId", obligation.id,
                        "contractId", obligation.contractId, "dueDate", obligation.dueDate.toString(), "daysUntilDue", days));
                NotificationEntity saved = notifications.save(notification);
                audit.record(obligation.organizationId, null, "REMINDER_CREATED", "NOTIFICATION", saved.id);
                if (emailEnabled) {
                    UserEntity user = memberUsers.get(member.userId);
                    if (user != null) emailOutbox.save(emailFor(saved, user, obligation, days));
                }
            }
        }
    }

    private EmailOutboxEntity emailFor(NotificationEntity notification, UserEntity user,
                                       ObligationEntity obligation, int days) {
        EmailOutboxEntity email = new EmailOutboxEntity();
        email.id = UUID.randomUUID().toString();
        email.organizationId = obligation.organizationId;
        email.notificationId = notification.id;
        email.recipient = user.email;
        email.subject = "Contract obligation due in " + days + (days == 1 ? " day" : " days");
        String description = escapeHtml(obligation.description);
        email.htmlContent = "<p>A contract obligation is due in " + days + (days == 1 ? " day" : " days")
                + ".</p><p><strong>" + description + "</strong></p><p>Due date: "
                + obligation.dueDate + "</p><p>Sign in to ContractIQ to review the source evidence.</p>";
        return email;
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
