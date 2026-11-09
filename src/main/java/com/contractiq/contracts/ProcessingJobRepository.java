package com.contractiq.contracts;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProcessingJobRepository extends JpaRepository<ProcessingJobEntity, String> {
    Optional<ProcessingJobEntity> findByIdAndOrganizationId(String id, String organizationId);
    Optional<ProcessingJobEntity> findFirstByDocumentIdAndOrganizationId(String documentId, String organizationId);
    long countByOrganizationIdAndState(String organizationId, String state);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from ProcessingJobEntity job where job.id = :id and job.organizationId = :organizationId")
    Optional<ProcessingJobEntity> findByIdAndOrganizationIdForUpdate(@Param("id") String id,
                                                                     @Param("organizationId") String organizationId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select job from ProcessingJobEntity job where job.documentId = :documentId and job.organizationId = :organizationId")
    Optional<ProcessingJobEntity> findFirstByDocumentIdAndOrganizationIdForUpdate(@Param("documentId") String documentId,
                                                                                   @Param("organizationId") String organizationId);
}
