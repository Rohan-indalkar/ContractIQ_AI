package com.contractiq.contracts;

import com.contractiq.identity.Role;
import com.contractiq.identity.UserPrincipal;
import com.contractiq.shared.AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DocumentServiceTenantTest {
    private final ContractRepository contracts = mock(ContractRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final ProcessingJobRepository jobs = mock(ProcessingJobRepository.class);
    private final OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    private final DocumentStorage storage = mock(DocumentStorage.class);
    private final AuditService audit = mock(AuditService.class);
    private final DocumentService service = new DocumentService(contracts, documents, jobs, outbox,
            storage, new ObjectMapper(), audit, 1024 * 1024);

    @Test
    void crossOrganizationContractCannotReceiveUploadedContent() {
        UserPrincipal principal = new UserPrincipal("user-a", "org-a", "a@example.test", Role.MEMBER);
        when(contracts.findByIdAndOrganizationId("contract-b", "org-a")).thenReturn(Optional.empty());
        var file = new MockMultipartFile("file", "agreement.txt", "text/plain", "private content".getBytes());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.upload(principal, "contract-b", file));

        assertEquals(404, error.getStatusCode().value());
        verify(storage, never()).put(anyString(), any(), anyString());
        verify(documents, never()).save(any());
    }

    @Test
    void validTextUploadStoresContentAndQueuesProcessing() {
        UserPrincipal principal = new UserPrincipal("user-a", "org-a", "a@example.test", Role.MEMBER);
        ContractEntity contract = new ContractEntity();
        contract.id = "contract-a";
        when(contracts.findByIdAndOrganizationId("contract-a", "org-a")).thenReturn(Optional.of(contract));
        when(documents.findByOrganizationIdAndSha256(eq("org-a"), anyString())).thenReturn(Optional.empty());
        byte[] content = "private content".getBytes(StandardCharsets.UTF_8);
        var file = new MockMultipartFile("file", "agreement.txt", "text/plain", content);

        var response = service.upload(principal, "contract-a", file);

        assertEquals("QUEUED", response.state());
        assertEquals("text/plain", response.mediaType());
        verify(storage).put(anyString(), org.mockito.ArgumentMatchers.eq(content), org.mockito.ArgumentMatchers.eq("text/plain"));
        verify(documents).save(any());
        verify(jobs).save(any());
        verify(outbox).save(any());
    }

    @Test
    void binaryContentWithTextExtensionIsRejected() {
        UserPrincipal principal = new UserPrincipal("user-a", "org-a", "a@example.test", Role.MEMBER);
        ContractEntity contract = new ContractEntity();
        contract.id = "contract-a";
        when(contracts.findByIdAndOrganizationId("contract-a", "org-a")).thenReturn(Optional.of(contract));
        var file = new MockMultipartFile("file", "agreement.txt", "text/plain",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a});

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.upload(principal, "contract-a", file));

        assertEquals(415, error.getStatusCode().value());
        verify(storage, never()).put(anyString(), any(), anyString());
    }

    @Test
    void validDocxUploadIsRecognized() throws Exception {
        UserPrincipal principal = new UserPrincipal("user-a", "org-a", "a@example.test", Role.MEMBER);
        ContractEntity contract = new ContractEntity();
        contract.id = "contract-a";
        when(contracts.findByIdAndOrganizationId("contract-a", "org-a")).thenReturn(Optional.of(contract));
        when(documents.findByOrganizationIdAndSha256(eq("org-a"), anyString())).thenReturn(Optional.empty());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(("<?xml version=\"1.0\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>" +
                    "</Types>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"/>"
                    .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        var file = new MockMultipartFile("file", "agreement.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", output.toByteArray());

        var response = service.upload(principal, "contract-a", file);

        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", response.mediaType());
        verify(storage).put(anyString(), any(), anyString());
    }
}
