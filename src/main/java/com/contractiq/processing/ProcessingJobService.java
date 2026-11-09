package com.contractiq.processing;

import com.contractiq.contracts.OutboxEventEntity;
import com.contractiq.contracts.OutboxEventRepository;
import com.contractiq.contracts.ProcessingEvent;
import com.contractiq.contracts.ProcessingJobEntity;
import com.contractiq.contracts.ProcessingJobRepository;
import com.contractiq.identity.UserPrincipal;
import com.contractiq.shared.AuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ProcessingJobService {
    private final ProcessingJobRepository jobs;
    private final OutboxEventRepository outbox;
    private final ObjectMapper objectMapper;
    private final AuditService audit;

    public ProcessingJobService(ProcessingJobRepository jobs, OutboxEventRepository outbox, ObjectMapper objectMapper,
                                AuditService audit) {
        this.jobs = jobs;
        this.outbox = outbox;
        this.objectMapper = objectMapper;
        this.audit = audit;
    }

    @Transactional
    public JobDto get(UserPrincipal principal, String jobId) {
        audit.record(principal.organizationId(), principal.userId(), "PROCESSING_JOB_READ", "PROCESSING_JOB", jobId);
        return jobs.findByIdAndOrganizationId(jobId, principal.organizationId()).map(JobDto::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Processing job not found"));
    }

    @Transactional
    public JobDto retry(UserPrincipal principal, String jobId) {
        ProcessingJobEntity job = jobs.findByIdAndOrganizationId(jobId, principal.organizationId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Processing job not found"));
        if (!"FAILED".equals(job.state))
            throw new ResponseStatusException(CONFLICT, "Only failed processing jobs can be retried");
        if (job.attemptCount >= 8)
            throw new ResponseStatusException(CONFLICT, "Processing retry limit reached");
        job.state = "QUEUED";
        job.lastError = null;
        jobs.save(job);

        OutboxEventEntity event = new OutboxEventEntity();
        event.id = UUID.randomUUID().toString();
        event.aggregateType = "DOCUMENT";
        event.aggregateId = job.documentId;
        event.organizationId = principal.organizationId();
        event.eventType = "contract.document.uploaded";
        event.payloadJson = objectMapper.valueToTree(
                new ProcessingEvent(event.id, job.documentId, principal.organizationId()));
        outbox.save(event);
        audit.record(principal.organizationId(), principal.userId(), "PROCESSING_JOB_RETRIED", "PROCESSING_JOB", job.id);
        return JobDto.from(job);
    }

    public record JobDto(String id, String documentId, String state, int attempts, java.time.LocalDateTime createdAt,
                         java.time.LocalDateTime updatedAt) {
        static JobDto from(ProcessingJobEntity job) {
            return new JobDto(job.id, job.documentId, job.state, job.attemptCount, job.createdAt, job.updatedAt);
        }
    }
}
