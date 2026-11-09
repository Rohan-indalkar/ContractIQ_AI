package com.contractiq.contracts;

import com.contractiq.identity.Role;
import com.contractiq.identity.UserPrincipal;
import com.contractiq.shared.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ContractServiceTenantTest {
    private final ContractRepository contracts = mock(ContractRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final ContractService service = new ContractService(contracts, audit);

    @Test
    void lookupScopesContractToAuthenticatedOrganization() {
        UserPrincipal principal = new UserPrincipal("user-a", "org-a", "a@example.test", Role.MEMBER);
        when(contracts.findByIdAndOrganizationId("contract-b", "org-a")).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.get(principal, "contract-b"));

        assertEquals(404, error.getStatusCode().value());
        verify(contracts).findByIdAndOrganizationId("contract-b", "org-a");
        verify(contracts, never()).findById("contract-b");
    }

    @Test
    void tenantListUsesOnlyAuthenticatedOrganization() {
        UserPrincipal principal = new UserPrincipal("user-a", "org-a", "a@example.test", Role.MEMBER);
        when(contracts.findAllByOrganizationId(eq("org-a"), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        service.list(principal, org.springframework.data.domain.PageRequest.of(0, 20));
        verify(contracts).findAllByOrganizationId(eq("org-a"), any());
        verify(contracts, never()).findAll(any(org.springframework.data.domain.Pageable.class));
    }
}
