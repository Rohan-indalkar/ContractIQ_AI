package com.contractiq.risks;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiskRepository extends JpaRepository<RiskEntity, String> {
    List<RiskEntity> findAllByOrganizationIdAndContractIdOrderByCreatedAtDesc(String organizationId, String contractId);
    Optional<RiskEntity> findByIdAndOrganizationId(String id, String organizationId);
    List<RiskEntity> findAllByDocumentIdAndOrganizationId(String documentId, String organizationId);
    long countByOrganizationId(String organizationId);
    long countByOrganizationIdAndReviewStatus(String organizationId, String reviewStatus);
}
