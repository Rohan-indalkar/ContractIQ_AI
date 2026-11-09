package com.contractiq.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events")
public class AuditEventEntity {
    @Id @Column(length = 36) public String id;
    @Column(name = "organization_id", nullable = false, length = 36) public String organizationId;
    @Column(name = "actor_user_id", length = 36) public String actorUserId;
    @Column(nullable = false, length = 120) public String action;
    @Column(name = "resource_type", nullable = false, length = 100) public String resourceType;
    @Column(name = "resource_id", length = 36) public String resourceId;
    @Column(name = "correlation_id", length = 100) public String correlationId;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) public LocalDateTime createdAt;
}
