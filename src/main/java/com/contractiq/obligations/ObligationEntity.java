package com.contractiq.obligations;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "obligations")
public class ObligationEntity {
    @Id @Column(length = 36) public String id;
    @Column(name = "organization_id", nullable = false, length = 36) public String organizationId;
    @Column(name = "contract_id", nullable = false, length = 36) public String contractId;
    @Column(name = "document_id", nullable = false, length = 36) public String documentId;
    @Column(nullable = false, columnDefinition = "text") public String description;
    @Column(name = "due_date") public LocalDate dueDate;
    @Column(nullable = false, length = 32) public String status;
    @Column(name = "source_type", nullable = false, length = 32) public String sourceType = "MANUAL";
    @Column(name = "source_key", length = 64) public String sourceKey;
    @Column(name = "source_evidence", columnDefinition = "text") public String sourceEvidence;
    @Column(name = "source_page") public Integer sourcePage;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) public LocalDateTime createdAt;
}
