package com.contractiq.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembershipRepository extends JpaRepository<MembershipEntity, String> {
    Optional<MembershipEntity> findByUserIdAndOrganizationId(String userId, String organizationId);
    Optional<MembershipEntity> findFirstByUserIdOrderByCreatedAtAsc(String userId);
    java.util.List<MembershipEntity> findAllByOrganizationId(String organizationId);
}
