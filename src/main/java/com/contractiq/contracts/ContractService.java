package com.contractiq.contracts;

import com.contractiq.identity.UserPrincipal;
import com.contractiq.shared.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ContractService {
    private final ContractRepository contracts;
    private final AuditService audit;

    public ContractService(ContractRepository contracts, AuditService audit) {
        this.contracts = contracts;
        this.audit = audit;
    }

    @Transactional
    public Page<ContractDto> list(UserPrincipal user, Pageable pageable) {
        audit.record(user.organizationId(), user.userId(), "CONTRACTS_LISTED", "CONTRACT", null);
        return contracts.findAllByOrganizationId(user.organizationId(), pageable).map(ContractDto::from);
    }

    @Transactional
    public Page<ContractDto> list(UserPrincipal user, Pageable pageable, ContractFilters filters) {
        audit.record(user.organizationId(), user.userId(), "CONTRACTS_LISTED", "CONTRACT", null);
        var specification = (org.springframework.data.jpa.domain.Specification<ContractEntity>) (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(builder.equal(root.get("organizationId"), user.organizationId()));
            if (filters.search() != null && !filters.search().isBlank()) {
                String search = "%" + filters.search().trim().toLowerCase(java.util.Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("title")), search),
                        builder.like(builder.lower(root.get("counterparty")), search)));
            }
            if (filters.counterparty() != null && !filters.counterparty().isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("counterparty")),
                        "%" + filters.counterparty().trim().toLowerCase(java.util.Locale.ROOT) + "%"));
            }
            if (filters.contractType() != null && !filters.contractType().isBlank())
                predicates.add(builder.equal(builder.lower(root.get("contractType")),
                        filters.contractType().trim().toLowerCase(java.util.Locale.ROOT)));
            if (filters.status() != null && !filters.status().isBlank())
                predicates.add(builder.equal(root.get("status"), filters.status().trim().toUpperCase(java.util.Locale.ROOT)));
            if (filters.createdFrom() != null)
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), filters.createdFrom().atStartOfDay()));
            if (filters.createdTo() != null)
                predicates.add(builder.lessThan(root.get("createdAt"), filters.createdTo().plusDays(1).atStartOfDay()));
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return contracts.findAll(specification, pageable).map(ContractDto::from);
    }

    @Transactional
    public ContractDto create(UserPrincipal user, CreateContractRequest request) {
        ContractEntity contract = new ContractEntity();
        contract.id = UUID.randomUUID().toString();
        contract.organizationId = user.organizationId();
        contract.createdBy = user.userId();
        contract.title = request.title().trim();
        contract.counterparty = clean(request.counterparty());
        contract.contractType = clean(request.contractType());
        contract.status = "ACTIVE";
        ContractEntity saved = contracts.save(contract);
        audit.record(user.organizationId(), user.userId(), "CONTRACT_CREATED", "CONTRACT", saved.id);
        return ContractDto.from(saved);
    }

    @Transactional
    public ContractDto get(UserPrincipal user, String contractId) {
        audit.record(user.organizationId(), user.userId(), "CONTRACT_READ", "CONTRACT", contractId);
        return contracts.findByIdAndOrganizationId(contractId, user.organizationId())
                .map(ContractDto::from).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Contract not found"));
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record CreateContractRequest(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 240) String title,
            @jakarta.validation.constraints.Size(max = 240) String counterparty,
            @jakarta.validation.constraints.Size(max = 100) String contractType) {}

    public record ContractFilters(String search, String counterparty, String contractType, String status,
                                  java.time.LocalDate createdFrom, java.time.LocalDate createdTo) {}

    public record ContractDto(String id, String title, String counterparty, String contractType,
                              String status, java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {
        static ContractDto from(ContractEntity e) {
            return new ContractDto(e.id, e.title, e.counterparty, e.contractType, e.status, e.createdAt, e.updatedAt);
        }
    }
}
