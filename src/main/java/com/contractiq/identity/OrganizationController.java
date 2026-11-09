package com.contractiq.identity;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations/current/members")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class OrganizationController {
    private final OrganizationService organizations;

    public OrganizationController(OrganizationService organizations) {
        this.organizations = organizations;
    }

    @GetMapping
    List<OrganizationService.MemberDto> members(@AuthenticationPrincipal UserPrincipal principal) {
        return organizations.members(principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER','ADMIN')")
    OrganizationService.MemberDto add(@AuthenticationPrincipal UserPrincipal principal,
                                      @Valid @RequestBody OrganizationService.AddMemberRequest request) {
        return organizations.addMember(principal, request.email(), request.role());
    }
}
