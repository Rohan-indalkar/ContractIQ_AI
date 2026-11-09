package com.contractiq.retrieval;

import com.contractiq.identity.UserPrincipal;
import com.contractiq.shared.AuditService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class RetrievalService {
    private final EmbeddingModel embeddings;
    private final TenantVectorIndex vectors;
    private final AuditService audit;

    public RetrievalService(EmbeddingModel embeddings, TenantVectorIndex vectors, AuditService audit) {
        this.embeddings = embeddings;
        this.vectors = vectors;
        this.audit = audit;
    }

    @org.springframework.transaction.annotation.Transactional
    public List<TenantVectorIndex.Evidence> search(UserPrincipal user, String query, Set<String> contractIds, int limit) {
        if (query == null || query.isBlank() || query.length() > 4000) {
            throw new ResponseStatusException(BAD_REQUEST, "query must contain 1-4000 characters");
        }
        float[] vector;
        try {
            vector = embeddings.embed(query);
        } catch (Exception exception) {
            org.slf4j.LoggerFactory.getLogger(RetrievalService.class).warn(
                    "Embedding provider unavailable exceptionType={}", exception.getClass().getSimpleName());
            throw new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Embedding provider is unavailable");
        }
        List<TenantVectorIndex.Evidence> result = vectors.search(vector, user.organizationId(), contractIds, limit);
        audit.record(user.organizationId(), user.userId(), "SEMANTIC_SEARCH", "SEARCH", null);
        return result;
    }
}
