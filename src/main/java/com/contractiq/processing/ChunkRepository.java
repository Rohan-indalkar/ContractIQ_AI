package com.contractiq.processing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChunkRepository extends JpaRepository<ChunkEntity, String> {
    List<ChunkEntity> findAllByDocumentIdAndOrganizationIdOrderByChunkIndex(String documentId, String organizationId);
    Optional<ChunkEntity> findByIdAndOrganizationId(String id, String organizationId);
    boolean existsByDocumentIdAndOrganizationIdAndPageNumber(String documentId, String organizationId, Integer pageNumber);
}
