package com.contractiq.processing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_chunks")
public class ChunkEntity {
    @Id @Column(length = 36) public String id;
    @Column(name = "organization_id", nullable = false, length = 36) public String organizationId;
    @Column(name = "document_id", nullable = false, length = 36) public String documentId;
    @Column(name = "chunk_index", nullable = false) public int chunkIndex;
    @Column(name = "page_number") public Integer pageNumber;
    @Column(nullable = false, columnDefinition = "mediumtext") public String content;
    @Column(name = "vector_id", length = 100) public String vectorId;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) public LocalDateTime createdAt;
}
