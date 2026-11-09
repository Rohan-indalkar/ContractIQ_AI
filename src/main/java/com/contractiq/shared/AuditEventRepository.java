package com.contractiq.shared;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, String> {
    Page<AuditEventEntity> findAllByOrganizationIdOrderByCreatedAtDesc(String organizationId, Pageable pageable);
}
