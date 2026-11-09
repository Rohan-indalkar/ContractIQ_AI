package com.contractiq.contracts;

import com.contractiq.identity.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contracts")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class ContractController {
    private final ContractService service;

    public ContractController(ContractService service) {
        this.service = service;
    }

    @GetMapping
    Page<ContractService.ContractDto> list(@AuthenticationPrincipal UserPrincipal user,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @RequestParam(required = false) String search,
                                            @RequestParam(required = false) String counterparty,
                                            @RequestParam(required = false) String contractType,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(
                                                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                                            java.time.LocalDate createdFrom,
                                            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(
                                                    iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                                            java.time.LocalDate createdTo) {
        if (page < 0 || size < 1 || size > 100) throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST, "page must be non-negative and size must be between 1 and 100");
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "createdFrom must not be after createdTo");
        if (tooLong(search, 240) || tooLong(counterparty, 240) || tooLong(contractType, 100) || tooLong(status, 32))
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A contract filter exceeds its maximum length");
        return service.list(user, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")),
                new ContractService.ContractFilters(search, counterparty, contractType, status, createdFrom, createdTo));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN','MEMBER')")
    ContractService.ContractDto create(@AuthenticationPrincipal UserPrincipal user,
                                       @Valid @RequestBody ContractService.CreateContractRequest request) {
        return service.create(user, request);
    }

    @GetMapping("/{contractId}")
    ContractService.ContractDto get(@AuthenticationPrincipal UserPrincipal user, @PathVariable String contractId) {
        return service.get(user, contractId);
    }

    private boolean tooLong(String value, int maximum) {
        return value != null && value.length() > maximum;
    }
}
