package com.contractiq.processing;

import com.contractiq.contracts.ContractRepository;
import com.contractiq.identity.UserPrincipal;
import com.contractiq.shared.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ExtractedClauseService {
    private final ExtractedClauseRepository clauses;
    private final ContractRepository contracts;
    private final AuditService audit;

    public ExtractedClauseService(ExtractedClauseRepository clauses, ContractRepository contracts, AuditService audit) {
        this.clauses = clauses;
        this.contracts = contracts;
        this.audit = audit;
    }

    @Transactional
    public List<ClauseDto> list(UserPrincipal principal, String contractId, String clauseType) {
        contracts.findByIdAndOrganizationId(contractId, principal.organizationId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Contract not found"));
        audit.record(principal.organizationId(), principal.userId(), "CLAUSES_LISTED", "CONTRACT", contractId);
        List<ExtractedClauseEntity> result = clauseType == null || clauseType.isBlank()
                ? clauses.findAllByContractIdAndOrganizationIdOrderByCreatedAtDesc(contractId, principal.organizationId())
                : clauses.findAllByContractIdAndOrganizationIdAndClauseTypeOrderByCreatedAtDesc(contractId,
                        principal.organizationId(), clauseType.trim().toUpperCase(java.util.Locale.ROOT));
        return result.stream().map(ClauseDto::from).toList();
    }

    public record ClauseDto(String id, String documentId, String type, com.fasterxml.jackson.databind.JsonNode value,
                            Integer pageNumber, String evidence, java.time.LocalDateTime createdAt) {
        static ClauseDto from(ExtractedClauseEntity entity) {
            return new ClauseDto(entity.id, entity.documentId, entity.clauseType, entity.valueJson,
                    entity.pageNumber, entity.evidenceText, entity.createdAt);
        }
    }
}
