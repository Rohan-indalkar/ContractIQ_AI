package com.contractiq.notifications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {
    List<NotificationEntity> findAllByOrganizationIdAndUserIdOrderByCreatedAtDesc(String organizationId, String userId);
    Optional<NotificationEntity> findByIdAndOrganizationIdAndUserId(String id, String organizationId, String userId);
    boolean existsByUserIdAndObligationIdAndReminderOffset(String userId, String obligationId, Integer reminderOffset);
}
