package com.contractiq.risks;

import com.contractiq.contracts.ContractRepository;
import com.contractiq.identity.UserPrincipal;
import com.contractiq.processing.ExtractedPage;
import com.contractiq.shared.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class RiskService {
    private final RiskRepository risks;
    private final ContractRepository contracts;
    private final DeterministicRiskAnalyzer analyzer;
    private final AuditService audit;

    public RiskService(RiskRepository risks, ContractRepository contracts, DeterministicRiskAnalyzer analyzer, AuditService audit) {
        this.risks = risks;
        this.contracts = contracts;
        this.analyzer = analyzer;
        this.audit = audit;
    }

    @Transactional
    public void replaceForDocument(String organizationId, String contractId, String documentId, List<ExtractedPage> pages) {
        risks.deleteAll(risks.findAllByDocumentIdAndOrganizationId(documentId, organizationId));
        List<RiskEntity> analyzed = analyzer.analyze(organizationId, contractId, documentId, pages);
        risks.saveAll(analyzed);
        audit.record(organizationId, null, "RISKS_ANALYZED", "DOCUMENT", documentId);
    }

    @Transactional
    public List<RiskDto> list(UserPrincipal principal, String contractId) {
        contracts.findByIdAndOrganizationId(contractId, principal.organizationId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Contract not found"));
        audit.record(principal.organizationId(), principal.userId(), "RISKS_LISTED", "CONTRACT", contractId);
        return risks.findAllByOrganizationIdAndContractIdOrderByCreatedAtDesc(principal.organizationId(), contractId)
                .stream().map(RiskDto::from).toList();
    }

    @Transactional
    public RiskDto review(UserPrincipal principal, String riskId, ReviewRequest request) {
        RiskEntity risk = risks.findByIdAndOrganizationId(riskId, principal.organizationId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Risk finding not found"));
        if (!List.of("CONFIRMED", "DISMISSED").contains(request.status()))
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "status must be CONFIRMED or DISMISSED");
        risk.reviewStatus = request.status();
        risk.reviewedBy = principal.userId();
        risks.save(risk);
        audit.record(principal.organizationId(), principal.userId(), "RISK_REVIEWED", "RISK", risk.id);
        return RiskDto.from(risk);
    }

    public record RiskDto(String id, String contractId, String documentId, String category, String severity,
                          String description, String evidenceText, Integer pageNumber, String reviewStatus,
                          java.time.LocalDateTime createdAt) {
        static RiskDto from(RiskEntity risk) {
            return new RiskDto(risk.id, risk.contractId, risk.documentId, risk.category, risk.severity,
                    risk.description, risk.evidenceText, risk.pageNumber, risk.reviewStatus, risk.createdAt);
        }
    }
    public record ReviewRequest(@jakarta.validation.constraints.NotBlank String status) {}
}
