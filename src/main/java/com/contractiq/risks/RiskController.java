package com.contractiq.risks;

import com.contractiq.identity.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class RiskController {
    private final RiskService risks;

    public RiskController(RiskService risks) {
        this.risks = risks;
    }

    @GetMapping("/contracts/{contractId}/risks")
    List<RiskService.RiskDto> list(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String contractId) {
        return risks.list(principal, contractId);
    }

    @PatchMapping("/risks/{riskId}/review")
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MEMBER')")
    RiskService.RiskDto review(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String riskId,
                               @Valid @RequestBody RiskService.ReviewRequest request) {
        return risks.review(principal, riskId, request);
    }
}
