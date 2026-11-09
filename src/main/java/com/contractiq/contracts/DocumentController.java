package com.contractiq.contracts;

import com.contractiq.identity.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.core.io.InputStreamResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/v1/contracts/{contractId}/documents")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class DocumentController {
    private final DocumentService documents;

    public DocumentController(DocumentService documents) {
        this.documents = documents;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MEMBER')")
    DocumentService.UploadResponse upload(@AuthenticationPrincipal UserPrincipal user, @PathVariable String contractId,
                                          @RequestPart("file") MultipartFile file) {
        return documents.upload(user, contractId, file);
    }

    @GetMapping
    List<DocumentService.DocumentDto> list(@AuthenticationPrincipal UserPrincipal user, @PathVariable String contractId) {
        return documents.list(user, contractId);
    }

    @GetMapping("/{documentId}/download")
    ResponseEntity<InputStreamResource> download(@AuthenticationPrincipal UserPrincipal user,
                                                  @PathVariable String documentId) {
        DocumentService.DocumentDownload file = documents.download(user, documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mediaType()))
                .contentLength(file.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.fileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(file.stream()));
    }
}
