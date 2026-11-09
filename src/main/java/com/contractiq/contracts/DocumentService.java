package com.contractiq.contracts;

import com.contractiq.identity.UserPrincipal;
import com.contractiq.shared.AuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
public class DocumentService {
    private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
            "application/pdf", ".pdf",
            "text/plain", ".txt",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");
    private final ContractRepository contracts;
    private final DocumentRepository documents;
    private final ProcessingJobRepository jobs;
    private final OutboxEventRepository outbox;
    private final DocumentStorage storage;
    private final ObjectMapper objectMapper;
    private final AuditService audit;
    private final long maxBytes;

    public DocumentService(ContractRepository contracts, DocumentRepository documents, ProcessingJobRepository jobs,
                           OutboxEventRepository outbox, DocumentStorage storage, ObjectMapper objectMapper,
                           AuditService audit,
                           @Value("${app.upload.max-bytes}") long maxBytes) {
        this.contracts = contracts;
        this.documents = documents;
        this.jobs = jobs;
        this.outbox = outbox;
        this.storage = storage;
        this.objectMapper = objectMapper;
        this.audit = audit;
        this.maxBytes = maxBytes;
    }

    @Transactional
    public UploadResponse upload(UserPrincipal user, String contractId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ResponseStatusException(BAD_REQUEST, "A document file is required");
        if (file.getSize() > maxBytes) throw new ResponseStatusException(PAYLOAD_TOO_LARGE, "Upload exceeds the configured limit");
        ContractEntity contract = contracts.findByIdAndOrganizationId(contractId, user.organizationId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Contract not found"));
        byte[] content;
        String detectedType;
        try {
            content = file.getBytes();
            detectedType = new Tika().detect(content, safeFilename(file.getOriginalFilename()));
        } catch (IOException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "Unable to read uploaded document");
        }
        String extension = ALLOWED_EXTENSIONS.get(detectedType);
        if (extension == null || !safeFilename(file.getOriginalFilename()).toLowerCase(java.util.Locale.ROOT).endsWith(extension)) {
            throw new ResponseStatusException(UNSUPPORTED_MEDIA_TYPE, "Only valid PDF, DOCX, and TXT documents are supported");
        }
        String checksum = sha256(content);
        if (documents.findByOrganizationIdAndSha256(user.organizationId(), checksum).isPresent()) {
            throw new ResponseStatusException(CONFLICT, "This document already exists in the organization");
        }

        String documentId = UUID.randomUUID().toString();
        String objectKey = user.organizationId() + "/" + documentId + extension;
        storage.put(objectKey, content, detectedType);
        DocumentEntity document = new DocumentEntity();
        document.id = documentId;
        document.organizationId = user.organizationId();
        document.contractId = contract.id;
        document.fileName = safeFilename(file.getOriginalFilename());
        document.mediaType = detectedType;
        document.byteSize = content.length;
        document.sha256 = checksum;
        document.objectKey = objectKey;
        documents.save(document);

        ProcessingJobEntity job = new ProcessingJobEntity();
        job.id = UUID.randomUUID().toString();
        job.organizationId = user.organizationId();
        job.documentId = document.id;
        job.state = "QUEUED";
        jobs.save(job);

        OutboxEventEntity event = new OutboxEventEntity();
        event.id = UUID.randomUUID().toString();
        event.aggregateType = "DOCUMENT";
        event.aggregateId = document.id;
        event.organizationId = user.organizationId();
        event.eventType = "contract.document.uploaded";
        event.payloadJson = objectMapper.valueToTree(new ProcessingEvent(event.id, document.id, user.organizationId()));
        outbox.save(event);
        audit.record(user.organizationId(), user.userId(), "DOCUMENT_UPLOADED", "DOCUMENT", document.id);
        return new UploadResponse(document.id, job.id, job.state, document.fileName, document.mediaType, document.byteSize);
    }

    @Transactional
    public DocumentDownload download(UserPrincipal user, String documentId) {
        DocumentEntity document = documents.findByIdAndOrganizationId(documentId, user.organizationId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Document not found"));
        audit.record(user.organizationId(), user.userId(), "DOCUMENT_DOWNLOADED", "DOCUMENT", document.id);
        return new DocumentDownload(document.fileName, document.mediaType, document.byteSize, storage.get(document.objectKey));
    }

    @Transactional
    public java.util.List<DocumentDto> list(UserPrincipal user, String contractId) {
        contracts.findByIdAndOrganizationId(contractId, user.organizationId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Contract not found"));
        audit.record(user.organizationId(), user.userId(), "DOCUMENTS_LISTED", "CONTRACT", contractId);
        return documents.findAllByContractIdAndOrganizationId(contractId, user.organizationId()).stream()
                .map(document -> new DocumentDto(document.id, document.fileName, document.mediaType, document.byteSize,
                        document.createdAt)).toList();
    }

    private String safeFilename(String fileName) {
        if (fileName == null) return "document";
        String base = java.nio.file.Paths.get(fileName.replace("\\", "/")).getFileName().toString();
        String clean = base.replaceAll("[^A-Za-z0-9._ -]", "_");
        return clean.length() > 255 ? clean.substring(clean.length() - 255) : clean;
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }

    public record UploadResponse(String documentId, String processingJobId, String state, String fileName, String mediaType, long size) {}
    public record DocumentDto(String id, String fileName, String mediaType, long size, java.time.LocalDateTime createdAt) {}
    public record DocumentDownload(String fileName, String mediaType, long size, InputStream stream) {}
}
