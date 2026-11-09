package com.contractiq.processing;

import com.contractiq.identity.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processing/jobs")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class ProcessingJobController {
    private final ProcessingJobService service;

    public ProcessingJobController(ProcessingJobService service) {
        this.service = service;
    }

    @GetMapping("/{jobId}")
    ProcessingJobService.JobDto get(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String jobId) {
        return service.get(principal, jobId);
    }

    @PostMapping("/{jobId}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MEMBER')")
    ProcessingJobService.JobDto retry(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String jobId) {
        return service.retry(principal, jobId);
    }
}
