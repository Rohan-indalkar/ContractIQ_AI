package com.contractiq.contracts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {
    @Id @Column(length = 36) public String id;
    @Column(name = "aggregate_type", nullable = false, length = 100) public String aggregateType;
    @Column(name = "aggregate_id", nullable = false, length = 36) public String aggregateId;
    @Column(name = "organization_id", nullable = false, length = 36) public String organizationId;
    @Column(name = "event_type", nullable = false, length = 120) public String eventType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", nullable = false, columnDefinition = "json") public JsonNode payloadJson;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) public LocalDateTime createdAt;
    @Column(name = "published_at") public LocalDateTime publishedAt;
}
