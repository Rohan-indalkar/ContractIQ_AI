package com.contractiq.shared;

import com.contractiq.identity.UserPrincipal;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {
    private final AuditEventRepository events;

    public AuditService(AuditEventRepository events) {
        this.events = events;
    }

    @Transactional
    public void record(String organizationId, String actorId, String action, String resourceType, String resourceId) {
        AuditEventEntity event = new AuditEventEntity();
        event.id = UUID.randomUUID().toString();
        event.organizationId = organizationId;
        event.actorUserId = actorId;
        event.action = action;
        event.resourceType = resourceType;
        event.resourceId = resourceId;
        event.correlationId = MDC.get("correlationId");
        events.save(event);
    }

    @Transactional
    public Page<AuditDto> list(UserPrincipal principal, Pageable pageable) {
        record(principal.organizationId(), principal.userId(), "AUDIT_EVENTS_LISTED", "AUDIT_EVENT", null);
        return events.findAllByOrganizationIdOrderByCreatedAtDesc(principal.organizationId(), pageable).map(event ->
                new AuditDto(event.id, event.actorUserId, event.action, event.resourceType, event.resourceId,
                        event.correlationId, event.createdAt));
    }

    public record AuditDto(String id, String actorUserId, String action, String resourceType,
                           String resourceId, String correlationId, java.time.LocalDateTime createdAt) {}
}
