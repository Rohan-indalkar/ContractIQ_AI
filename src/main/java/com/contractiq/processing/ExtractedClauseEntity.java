package com.contractiq.processing;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "extracted_clauses")
public class ExtractedClauseEntity {
    @Id @Column(length = 36) public String id;
    @Column(name = "organization_id", nullable = false, length = 36) public String organizationId;
    @Column(name = "contract_id", nullable = false, length = 36) public String contractId;
    @Column(name = "document_id", nullable = false, length = 36) public String documentId;
    @Column(name = "clause_type", nullable = false, length = 100) public String clauseType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_json", nullable = false, columnDefinition = "json") public JsonNode valueJson;
    @Column(name = "page_number") public Integer pageNumber;
    @Column(name = "evidence_text", nullable = false, columnDefinition = "text") public String evidenceText;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) public LocalDateTime createdAt;
}
