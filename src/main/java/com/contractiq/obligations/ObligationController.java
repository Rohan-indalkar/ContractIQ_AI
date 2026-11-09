package com.contractiq.obligations;

import com.contractiq.identity.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class ObligationController {
    private final ObligationService obligations;

    public ObligationController(ObligationService obligations) {
        this.obligations = obligations;
    }

    @GetMapping("/obligations")
    List<ObligationService.ObligationDto> list(@AuthenticationPrincipal UserPrincipal principal) {
        return obligations.list(principal);
    }

    @PostMapping("/contracts/{contractId}/obligations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MEMBER')")
    ObligationService.ObligationDto create(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String contractId,
                                           @Valid @RequestBody ObligationService.CreateRequest request) {
        return obligations.create(principal, contractId, request);
    }

    @PatchMapping("/obligations/{obligationId}")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MEMBER')")
    ObligationService.ObligationDto update(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String obligationId,
                                           @Valid @RequestBody ObligationService.UpdateRequest request) {
        return obligations.updateStatus(principal, obligationId, request);
    }
}
