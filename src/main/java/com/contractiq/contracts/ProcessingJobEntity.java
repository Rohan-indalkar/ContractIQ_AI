package com.contractiq.contracts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "processing_jobs")
public class ProcessingJobEntity {
    @Id @Column(length = 36) public String id;
    @Column(name = "organization_id", nullable = false, length = 36) public String organizationId;
    @Column(name = "document_id", nullable = false, length = 36) public String documentId;
    @Column(nullable = false, length = 32) public String state;
    @Column(name = "attempt_count", nullable = false) public int attemptCount;
    @Column(name = "last_error", length = 1000) public String lastError;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) public LocalDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) public LocalDateTime updatedAt;
}
