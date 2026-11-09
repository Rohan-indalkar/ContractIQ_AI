package com.contractiq.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_outbox")
public class EmailOutboxEntity {
    @Id @Column(length = 36) public String id;
    @Column(name = "organization_id", nullable = false, length = 36) public String organizationId;
    @Column(name = "notification_id", nullable = false, unique = true, length = 36) public String notificationId;
    @Column(nullable = false, length = 254) public String recipient;
    @Column(nullable = false, length = 240) public String subject;
    @Column(name = "html_content", nullable = false, columnDefinition = "mediumtext") public String htmlContent;
    @Column(name = "attempt_count", nullable = false) public int attemptCount;
    @Column(name = "last_error", length = 1000) public String lastError;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) public LocalDateTime createdAt;
    @Column(name = "sent_at") public LocalDateTime sentAt;
}
