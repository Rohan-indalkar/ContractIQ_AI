package com.contractiq.obligations;

import com.contractiq.contracts.ContractRepository;
import com.contractiq.contracts.DocumentRepository;
import com.contractiq.identity.UserPrincipal;
import com.contractiq.processing.ChunkRepository;
import com.contractiq.shared.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ObligationService {
    private final ObligationRepository obligations;
    private final ContractRepository contracts;
    private final DocumentRepository documents;
    private final ChunkRepository chunks;
    private final AuditService audit;

    public ObligationService(ObligationRepository obligations, ContractRepository contracts,
                             DocumentRepository documents, ChunkRepository chunks, AuditService audit) {
        this.obligations = obligations;
        this.contracts = contracts;
        this.documents = documents;
        this.chunks = chunks;
        this.audit = audit;
    }

    @Transactional
    public List<ObligationDto> list(UserPrincipal principal) {
        audit.record(principal.organizationId(), principal.userId(), "OBLIGATIONS_LISTED", "OBLIGATION", null);
        return obligations.findAllByOrganizationIdOrderByDueDateAsc(principal.organizationId())
                .stream().map(ObligationDto::from).toList();
    }

    @Transactional
    public ObligationDto create(UserPrincipal principal, String contractId, CreateRequest request) {
        contracts.findByIdAndOrganizationId(contractId, principal.organizationId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Contract not found"));
        documents.findByIdAndOrganizationId(request.documentId(), principal.organizationId())
                .filter(document -> document.contractId.equals(contractId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Source document not found"));
        if (!chunks.existsByDocumentIdAndOrganizationIdAndPageNumber(request.documentId(), principal.organizationId(),
                request.sourcePage()))
            throw new ResponseStatusException(NOT_FOUND, "Evidence page not found for the source document");
        ObligationEntity obligation = new ObligationEntity();
        obligation.id = UUID.randomUUID().toString();
        obligation.organizationId = principal.organizationId();
        obligation.contractId = contractId;
        obligation.documentId = request.documentId();
        obligation.description = request.description().trim();
        obligation.dueDate = request.dueDate();
        obligation.sourcePage = request.sourcePage();
        obligation.status = "OPEN";
        obligation.sourceType = "MANUAL";
        obligations.save(obligation);
        audit.record(principal.organizationId(), principal.userId(), "OBLIGATION_CREATED", "OBLIGATION", obligation.id);
        return ObligationDto.from(obligation);
    }

    @Transactional
    public ObligationDto updateStatus(UserPrincipal principal, String id, UpdateRequest request) {
        ObligationEntity obligation = obligations.findByIdAndOrganizationId(id, principal.organizationId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Obligation not found"));
        if (!List.of("OPEN", "COMPLETED", "DISMISSED").contains(request.status()))
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid obligation status");
        obligation.status = request.status();
        obligations.save(obligation);
        audit.record(principal.organizationId(), principal.userId(), "OBLIGATION_STATUS_CHANGED", "OBLIGATION", obligation.id);
        return ObligationDto.from(obligation);
    }

    public record CreateRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 4000) String description,
                                LocalDate dueDate,
                                @jakarta.validation.constraints.NotBlank String documentId,
                                @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(1) Integer sourcePage) {}
    public record UpdateRequest(@jakarta.validation.constraints.NotBlank String status) {}
    public record ObligationDto(String id, String contractId, String documentId, String description, LocalDate dueDate,
                                String status, String sourceType, Integer sourcePage, String sourceEvidence,
                                java.time.LocalDateTime createdAt) {
        static ObligationDto from(ObligationEntity obligation) {
            return new ObligationDto(obligation.id, obligation.contractId, obligation.documentId, obligation.description,
                    obligation.dueDate, obligation.status, obligation.sourceType, obligation.sourcePage,
                    obligation.sourceEvidence, obligation.createdAt);
        }
    }
}
