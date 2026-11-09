package com.contractiq.notifications;

import com.contractiq.identity.UserPrincipal;
import com.contractiq.shared.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class NotificationService {
    private final NotificationRepository notifications;
    private final AuditService audit;

    public NotificationService(NotificationRepository notifications, AuditService audit) {
        this.notifications = notifications;
        this.audit = audit;
    }

    @Transactional
    public List<NotificationDto> list(UserPrincipal principal) {
        audit.record(principal.organizationId(), principal.userId(), "NOTIFICATIONS_LISTED", "NOTIFICATION", null);
        return notifications.findAllByOrganizationIdAndUserIdOrderByCreatedAtDesc(
                        principal.organizationId(), principal.userId()).stream().map(NotificationDto::from).toList();
    }

    @Transactional
    public NotificationDto markRead(UserPrincipal principal, String notificationId) {
        NotificationEntity notification = notifications.findByIdAndOrganizationIdAndUserId(
                        notificationId, principal.organizationId(), principal.userId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Notification not found"));
        if (notification.readAt == null) notification.readAt = java.time.LocalDateTime.now();
        notifications.save(notification);
        audit.record(principal.organizationId(), principal.userId(), "NOTIFICATION_READ", "NOTIFICATION", notification.id);
        return NotificationDto.from(notification);
    }

    public record NotificationDto(String id, String kind, com.fasterxml.jackson.databind.JsonNode payload, boolean read,
                                  java.time.LocalDateTime createdAt) {
        static NotificationDto from(NotificationEntity notification) {
            return new NotificationDto(notification.id, notification.kind, notification.payloadJson,
                    notification.readAt != null, notification.createdAt);
        }
    }
}
