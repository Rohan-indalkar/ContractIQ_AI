package com.contractiq.retrieval;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Component
public class QdrantVectorIndex implements TenantVectorIndex {
    private final RestClient client;
    private final String collection;
    private final AtomicInteger dimensions = new AtomicInteger();

    public QdrantVectorIndex(RestClient.Builder builder,
                             @Value("${app.vector.endpoint}") String endpoint,
                             @Value("${app.vector.collection}") String collection) {
        this.client = builder.baseUrl(endpoint).build();
        this.collection = collection;
    }

    @Override
    public void upsert(List<VectorPoint> points) {
        if (points.isEmpty()) return;
        try {
            ensureCollection(points.getFirst().vector().length);
            List<Map<String, Object>> records = points.stream().map(point -> {
                Map<String, Object> payload = new HashMap<>();
                payload.put("organization_id", point.organizationId());
                payload.put("document_id", point.documentId());
                payload.put("contract_id", point.contractId());
                payload.put("page_number", point.pageNumber());
                payload.put("content", point.content());
                return Map.<String, Object>of("id", point.id(), "vector", point.vector(), "payload", payload);
            }).toList();
            client.put().uri("/collections/{collection}/points?wait=true", collection)
                    .body(Map.of("points", records)).retrieve().toBodilessEntity();
        } catch (Exception exception) {
            throw providerUnavailable("Vector indexing is unavailable", exception);
        }
    }

    @Override
    public List<Evidence> search(float[] vector, String organizationId, Set<String> contractIds, int limit) {
        try {
            ensureCollection(vector.length);
            List<Map<String, Object>> must = new ArrayList<>();
            must.add(Map.of("key", "organization_id", "match", Map.of("value", organizationId)));
            if (contractIds != null && !contractIds.isEmpty()) {
                must.add(Map.of("key", "contract_id", "match", Map.of("any", contractIds)));
            }
            JsonNode response = client.post().uri("/collections/{collection}/points/query", collection)
                    .body(Map.of("query", vector, "filter", Map.of("must", must),
                            "limit", Math.min(Math.max(limit, 1), 20), "with_payload", true))
                    .retrieve().body(JsonNode.class);
            JsonNode results = response == null ? null : response.path("result").path("points");
            if (results == null || !results.isArray()) return List.of();
            List<Evidence> evidence = new ArrayList<>();
            for (JsonNode point : results) {
                JsonNode payload = point.path("payload");
                evidence.add(new Evidence(point.path("id").asText(), payload.path("document_id").asText(),
                        payload.path("contract_id").asText(),
                        payload.path("page_number").isNumber() ? payload.path("page_number").asInt() : null,
                        payload.path("content").asText(), point.path("score").asDouble()));
            }
            return evidence;
        } catch (Exception exception) {
            throw providerUnavailable("Vector retrieval is unavailable", exception);
        }
    }

    private void ensureCollection(int size) {
        int known = dimensions.get();
        if (known == size) return;
        try {
            client.get().uri("/collections/{collection}", collection).retrieve().toBodilessEntity();
            dimensions.compareAndSet(0, size);
            if (dimensions.get() != size) throw new IllegalStateException("Configured embedding dimension does not match Qdrant collection");
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) throw exception;
            client.put().uri("/collections/{collection}", collection)
                    .body(Map.of("vectors", Map.of("size", size, "distance", "Cosine")))
                    .retrieve().toBodilessEntity();
            dimensions.set(size);
        }
    }

    private ResponseStatusException providerUnavailable(String message, Exception cause) {
        org.slf4j.LoggerFactory.getLogger(QdrantVectorIndex.class).warn("{} exceptionType={}", message, cause.getClass().getSimpleName());
        return new ResponseStatusException(SERVICE_UNAVAILABLE, message);
    }
}
