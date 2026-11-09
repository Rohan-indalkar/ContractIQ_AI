package com.contractiq.retrieval;

import java.util.List;
import java.util.Set;

public interface TenantVectorIndex {
    void upsert(List<VectorPoint> points);
    List<Evidence> search(float[] vector, String organizationId, Set<String> contractIds, int limit);

    record VectorPoint(String id, float[] vector, String organizationId, String documentId,
                       String contractId, Integer pageNumber, String content) {}
    record Evidence(String chunkId, String documentId, String contractId, Integer pageNumber, String content, double score) {}
}
