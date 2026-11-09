package com.contractiq.processing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExtractedClauseRepository extends JpaRepository<ExtractedClauseEntity, String> {
    List<ExtractedClauseEntity> findAllByDocumentIdAndOrganizationIdOrderByPageNumberAsc(String documentId, String organizationId);
    List<ExtractedClauseEntity> findAllByContractIdAndOrganizationIdOrderByCreatedAtDesc(String contractId, String organizationId);
    List<ExtractedClauseEntity> findAllByContractIdAndOrganizationIdAndClauseTypeOrderByCreatedAtDesc(
            String contractId, String organizationId, String clauseType);
    void deleteAllByDocumentIdAndOrganizationId(String documentId, String organizationId);
}
