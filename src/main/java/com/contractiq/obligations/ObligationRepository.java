package com.contractiq.obligations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ObligationRepository extends JpaRepository<ObligationEntity, String> {
    List<ObligationEntity> findAllByOrganizationIdOrderByDueDateAsc(String organizationId);
    List<ObligationEntity> findAllByDueDateAndStatus(LocalDate dueDate, String status);
    void deleteAllByDocumentIdAndOrganizationIdAndSourceType(String documentId, String organizationId, String sourceType);
    List<ObligationEntity> findAllByDocumentIdAndOrganizationIdAndSourceType(
            String documentId, String organizationId, String sourceType);
    Optional<ObligationEntity> findByIdAndOrganizationId(String id, String organizationId);
}
