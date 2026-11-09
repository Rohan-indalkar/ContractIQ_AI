package com.contractiq.processing;

import com.contractiq.contracts.DocumentEntity;
import com.contractiq.contracts.DocumentRepository;
import com.contractiq.contracts.DocumentStorage;
import com.contractiq.contracts.ProcessingEvent;
import com.contractiq.retrieval.TenantVectorIndex;
import com.contractiq.risks.RiskService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProcessingService {
    private final DocumentRepository documents;
    private final ChunkRepository chunks;
    private final DocumentStorage storage;
    private final DocumentTextExtractor extractor;
    private final TextChunker chunker;
    private final EmbeddingModel embeddings;
    private final TenantVectorIndex vectors;
    private final RiskService risks;
    private final ProcessingStateService state;
    private final ClausePersistenceService clausePersistence;

    public ProcessingService(DocumentRepository documents, ChunkRepository chunks,
                             DocumentStorage storage, DocumentTextExtractor extractor, TextChunker chunker,
                             EmbeddingModel embeddings, TenantVectorIndex vectors, RiskService risks,
                             ProcessingStateService state, ClausePersistenceService clausePersistence) {
        this.documents = documents;
        this.chunks = chunks;
        this.storage = storage;
        this.extractor = extractor;
        this.chunker = chunker;
        this.embeddings = embeddings;
        this.vectors = vectors;
        this.risks = risks;
        this.state = state;
        this.clausePersistence = clausePersistence;
    }

    @KafkaListener(topics = "contract.document.uploaded", groupId = "contractiq-processing")
    @RetryableTopic(attempts = "4", backoff = @Backoff(delay = 1000, multiplier = 2))
    public void process(ProcessingEvent event) {
        try {
            processDocument(event);
        } catch (Exception exception) {
            state.failed(event, exception);
            throw new IllegalStateException("Document processing failed; message will be retried");
        }
    }

    public void retry(ProcessingEvent event) {
        process(event);
    }

    private void processDocument(ProcessingEvent event) throws IOException {
        var job = state.claim(event);
        if (job == null) return;
        DocumentEntity document = documents.findByIdAndOrganizationId(event.documentId(), event.organizationId())
                .orElseThrow(() -> new IllegalStateException("Document metadata is missing"));
        byte[] content;
        try (var stream = storage.get(document.objectKey)) {
            content = stream.readAllBytes();
        }
        List<ExtractedPage> pages = extractor.extract(content, document.mediaType);
        if (pages.stream().allMatch(page -> page.text().isBlank())) {
            throw new IllegalArgumentException("No extractable text was found; scanned documents require OCR");
        }
        List<ChunkEntity> newChunks = chunker.chunk(document.organizationId, document.id, pages);
        if (newChunks.isEmpty()) throw new IllegalArgumentException("Document contains no indexable text");

        List<TenantVectorIndex.VectorPoint> points = new ArrayList<>(newChunks.size());
        for (ChunkEntity chunk : newChunks) {
            float[] vector = embeddings.embed(chunk.content);
            points.add(new TenantVectorIndex.VectorPoint(chunk.vectorId, vector, chunk.organizationId, chunk.documentId,
                    document.contractId, chunk.pageNumber, chunk.content));
        }
        chunks.deleteAll(chunks.findAllByDocumentIdAndOrganizationIdOrderByChunkIndex(document.id, event.organizationId()));
        chunks.saveAll(newChunks);
        vectors.upsert(points);
        risks.replaceForDocument(document.organizationId, document.contractId, document.id, pages);
        clausePersistence.extractAndReplace(document.organizationId, document.contractId, document.id, pages);
        state.completed(job.id, event.organizationId());
    }
}
