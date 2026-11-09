package com.contractiq.identity;

import com.contractiq.shared.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class OrganizationService {
    private final MembershipRepository memberships;
    private final UserRepository users;
    private final AuditService audit;

    public OrganizationService(MembershipRepository memberships, UserRepository users, AuditService audit) {
        this.memberships = memberships;
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    public List<MemberDto> members(UserPrincipal principal) {
        audit.record(principal.organizationId(), principal.userId(), "MEMBERS_LISTED", "MEMBERSHIP", null);
        return memberships.findAllByOrganizationId(principal.organizationId()).stream()
                .map(membership -> users.findById(membership.userId)
                        .map(user -> new MemberDto(user.id, user.email, user.displayName, membership.role, membership.createdAt))
                        .orElse(null))
                .filter(java.util.Objects::nonNull).toList();
    }

    @Transactional
    public MemberDto addMember(UserPrincipal principal, String email, Role role) {
        if (role == Role.OWNER)
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Use ADMIN or MEMBER for additional accounts");
        UserEntity user = users.findByEmail(email.trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User account not found"));
        if (memberships.findByUserIdAndOrganizationId(user.id, principal.organizationId()).isPresent())
            throw new ResponseStatusException(CONFLICT, "User is already a member of this organization");
        MembershipEntity membership = new MembershipEntity();
        membership.id = UUID.randomUUID().toString();
        membership.organizationId = principal.organizationId();
        membership.userId = user.id;
        membership.role = role.name();
        memberships.save(membership);
        audit.record(principal.organizationId(), principal.userId(), "MEMBER_ADDED", "MEMBERSHIP", membership.id);
        return new MemberDto(user.id, user.email, user.displayName, membership.role, membership.createdAt);
    }

    public record MemberDto(String id, String email, String displayName, String role, java.time.LocalDateTime joinedAt) {}
    public record AddMemberRequest(@jakarta.validation.constraints.Email @jakarta.validation.constraints.NotBlank String email,
                                   @jakarta.validation.constraints.NotNull Role role) {}
}
