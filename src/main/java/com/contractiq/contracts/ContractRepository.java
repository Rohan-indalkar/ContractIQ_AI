package com.contractiq.contracts;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContractRepository extends JpaRepository<ContractEntity, String>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<ContractEntity> {
    Page<ContractEntity> findAllByOrganizationId(String organizationId, Pageable pageable);
    Optional<ContractEntity> findByIdAndOrganizationId(String id, String organizationId);
    long countByOrganizationId(String organizationId);
    long countByOrganizationIdAndStatus(String organizationId, String status);
}
