package com.contractiq.contracts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<DocumentEntity, String> {
    Optional<DocumentEntity> findByIdAndOrganizationId(String id, String organizationId);
    Optional<DocumentEntity> findByOrganizationIdAndSha256(String organizationId, String sha256);
    List<DocumentEntity> findAllByContractIdAndOrganizationId(String contractId, String organizationId);
    long countByOrganizationId(String organizationId);
}
