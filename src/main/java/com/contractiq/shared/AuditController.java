package com.contractiq.shared;

import com.contractiq.identity.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit-events")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class AuditController {
    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    Page<AuditService.AuditDto> list(@AuthenticationPrincipal UserPrincipal principal,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid pagination");
        return audit.list(principal, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }
}
