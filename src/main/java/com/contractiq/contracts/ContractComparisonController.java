package com.contractiq.contracts;

import com.contractiq.identity.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contracts/compare")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class ContractComparisonController {
    private final ContractComparisonService comparison;

    public ContractComparisonController(ContractComparisonService comparison) {
        this.comparison = comparison;
    }

    @PostMapping
    ContractComparisonService.Comparison compare(@AuthenticationPrincipal UserPrincipal principal,
                                                 @Valid @RequestBody CompareRequest request) {
        return comparison.compare(principal, request.firstContractId(), request.secondContractId());
    }

    public record CompareRequest(@NotBlank @Size(max = 36) String firstContractId,
                                 @NotBlank @Size(max = 36) String secondContractId) {}
}
