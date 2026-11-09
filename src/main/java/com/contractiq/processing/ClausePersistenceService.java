package com.contractiq.processing;

import com.contractiq.obligations.ObligationEntity;
import com.contractiq.obligations.ObligationRepository;
import com.contractiq.shared.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ClausePersistenceService {
    private final ExtractedClauseRepository clauses;
    private final ObligationRepository obligations;
    private final StructuredExtractionService extraction;
    private final AuditService audit;

    public ClausePersistenceService(ExtractedClauseRepository clauses, ObligationRepository obligations,
                                    StructuredExtractionService extraction, AuditService audit) {
        this.clauses = clauses;
        this.obligations = obligations;
        this.extraction = extraction;
        this.audit = audit;
    }

    @Transactional
    public void extractAndReplace(String organizationId, String contractId, String documentId, List<ExtractedPage> pages) {
        List<StructuredExtractionService.ExtractedClause> extractedClauses = new ArrayList<>();
        List<StructuredExtractionService.ExtractedObligation> extractedObligations = new ArrayList<>();
        for (ExtractedPage page : pages) {
            if (page.text().isBlank()) continue;
            var result = extraction.extract(page);
            extractedClauses.addAll(result.clauses());
            extractedObligations.addAll(result.obligations());
        }

        clauses.deleteAllByDocumentIdAndOrganizationId(documentId, organizationId);
        List<ExtractedClauseEntity> clauseEntities = extractedClauses.stream().map(clause -> {
            ExtractedClauseEntity entity = new ExtractedClauseEntity();
            entity.id = UUID.randomUUID().toString();
            entity.organizationId = organizationId;
            entity.contractId = contractId;
            entity.documentId = documentId;
            entity.clauseType = clause.type();
            entity.valueJson = clause.value();
            entity.pageNumber = clause.pageNumber();
            entity.evidenceText = clause.evidence();
            return entity;
        }).toList();
        clauses.saveAll(clauseEntities);

        var existingObligations = obligations.findAllByDocumentIdAndOrganizationIdAndSourceType(documentId, organizationId, "AI")
                .stream().collect(java.util.stream.Collectors.toMap(item -> item.sourceKey, item -> item));
        var seenKeys = new java.util.HashSet<String>();
        List<ObligationEntity> obligationEntities = extractedObligations.stream().map(item -> {
            String sourceKey = sourceKey(item);
            seenKeys.add(sourceKey);
            ObligationEntity entity = existingObligations.get(sourceKey);
            if (entity == null) {
                entity = new ObligationEntity();
                entity.id = UUID.randomUUID().toString();
                entity.organizationId = organizationId;
                entity.contractId = contractId;
                entity.documentId = documentId;
                entity.status = "OPEN";
                entity.sourceType = "AI";
                entity.sourceKey = sourceKey;
            }
            entity.description = item.description();
            entity.dueDate = item.dueDate();
            entity.sourcePage = item.pageNumber();
            entity.sourceEvidence = item.evidence();
            return entity;
        }).toList();
        existingObligations.forEach((key, obligation) -> {
            if (!seenKeys.contains(key) && "OPEN".equals(obligation.status)) obligation.status = "DISMISSED";
        });
        obligations.saveAll(obligationEntities);
        audit.record(organizationId, null, "DOCUMENT_ANALYZED", "DOCUMENT", documentId);
    }

    private String sourceKey(StructuredExtractionService.ExtractedObligation obligation) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256").digest(
                    (obligation.pageNumber() + "\n" + obligation.description() + "\n" + obligation.evidence())
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }
}
