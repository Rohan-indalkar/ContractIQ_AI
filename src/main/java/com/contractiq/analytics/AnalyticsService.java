package com.contractiq.analytics;

import com.contractiq.contracts.ContractRepository;
import com.contractiq.contracts.DocumentRepository;
import com.contractiq.contracts.ProcessingJobRepository;
import com.contractiq.identity.UserPrincipal;
import com.contractiq.risks.RiskRepository;
import com.contractiq.shared.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private final ContractRepository contracts;
    private final DocumentRepository documents;
    private final ProcessingJobRepository jobs;
    private final RiskRepository risks;
    private final AuditService audit;

    public AnalyticsService(ContractRepository contracts, DocumentRepository documents,
                            ProcessingJobRepository jobs, RiskRepository risks, AuditService audit) {
        this.contracts = contracts;
        this.documents = documents;
        this.jobs = jobs;
        this.risks = risks;
        this.audit = audit;
    }

    @Transactional
    public Overview overview(UserPrincipal principal) {
        String org = principal.organizationId();
        audit.record(org, principal.userId(), "ANALYTICS_READ", "ANALYTICS", null);
        return new Overview(contracts.countByOrganizationId(org), contracts.countByOrganizationIdAndStatus(org, "ACTIVE"),
                documents.countByOrganizationId(org), jobs.countByOrganizationIdAndState(org, "QUEUED"),
                jobs.countByOrganizationIdAndState(org, "PROCESSING"), jobs.countByOrganizationIdAndState(org, "FAILED"),
                risks.countByOrganizationId(org), risks.countByOrganizationIdAndReviewStatus(org, "PENDING"));
    }

    public record Overview(long contractCount, long activeContractCount, long documentCount,
                           long queuedJobs, long processingJobs, long failedJobs, long riskFindings,
                           long pendingRiskReviews) {}
}
