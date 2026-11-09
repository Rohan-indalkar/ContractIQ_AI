package com.contractiq.risks;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "risks")
public class RiskEntity {
    @Id @Column(length = 36) public String id;
    @Column(name = "organization_id", nullable = false, length = 36) public String organizationId;
    @Column(name = "contract_id", nullable = false, length = 36) public String contractId;
    @Column(name = "document_id", nullable = false, length = 36) public String documentId;
    @Column(nullable = false, length = 100) public String category;
    @Column(nullable = false, length = 32) public String severity;
    @Column(nullable = false, columnDefinition = "text") public String description;
    @Column(name = "evidence_text", nullable = false, columnDefinition = "text") public String evidenceText;
    @Column(name = "page_number") public Integer pageNumber;
    @Column(name = "review_status", nullable = false, length = 32) public String reviewStatus;
    @Column(name = "reviewed_by", length = 36) public String reviewedBy;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) public LocalDateTime createdAt;
}
