package com.contractiq.processing;

import com.contractiq.contracts.ProcessingEvent;
import com.contractiq.contracts.ProcessingJobEntity;
import com.contractiq.contracts.ProcessingJobRepository;
import com.contractiq.shared.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ProcessingStateService {
    private final ProcessingJobRepository jobs;
    private final AuditService audit;

    public ProcessingStateService(ProcessingJobRepository jobs, AuditService audit) {
        this.jobs = jobs;
        this.audit = audit;
    }

    @Transactional
    public ProcessingJobEntity claim(ProcessingEvent event) {
        ProcessingJobEntity job = jobs.findFirstByDocumentIdAndOrganizationIdForUpdate(
                        event.documentId(), event.organizationId())
                .orElseThrow(() -> new IllegalStateException("Processing job is missing"));
        if ("COMPLETED".equals(job.state) || "PROCESSING".equals(job.state)) return null;
        job.attemptCount++;
        job.state = "PROCESSING";
        job.lastError = null;
        audit.record(event.organizationId(), null, "PROCESSING_STARTED", "PROCESSING_JOB", job.id);
        return jobs.save(job);
    }

    @Transactional
    public void completed(String id, String organizationId) {
        jobs.findByIdAndOrganizationIdForUpdate(id, organizationId).ifPresent(job -> {
            job.state = "COMPLETED";
            job.lastError = null;
            jobs.save(job);
            audit.record(organizationId, null, "PROCESSING_COMPLETED", "PROCESSING_JOB", job.id);
        });
    }

    @Transactional
    public void failed(ProcessingEvent event, Exception exception) {
        jobs.findFirstByDocumentIdAndOrganizationIdForUpdate(event.documentId(), event.organizationId()).ifPresent(job -> {
            if ("COMPLETED".equals(job.state)) return;
            job.state = "FAILED";
            job.lastError = "PROCESSING_FAILURE:" + exception.getClass().getSimpleName();
            job.updatedAt = LocalDateTime.now();
            jobs.save(job);
            audit.record(event.organizationId(), null, "PROCESSING_FAILED", "PROCESSING_JOB", job.id);
        });
    }
}
