package com.contractiq.contracts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
public class DocumentEntity {
    @Id @Column(length = 36) public String id;
    @Column(name = "organization_id", nullable = false, length = 36) public String organizationId;
    @Column(name = "contract_id", nullable = false, length = 36) public String contractId;
    @Column(name = "file_name", nullable = false, length = 255) public String fileName;
    @Column(name = "media_type", nullable = false, length = 160) public String mediaType;
    @Column(name = "byte_size", nullable = false) public long byteSize;
    @Column(nullable = false, length = 64) public String sha256;
    @Column(name = "object_key", nullable = false, length = 512) public String objectKey;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) public LocalDateTime createdAt;
}
