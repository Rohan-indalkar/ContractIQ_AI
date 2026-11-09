package com.contractiq.identity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.contractiq.shared.AuditService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.CONFLICT;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserRepository users;
    private final OrganizationRepository organizations;
    private final MembershipRepository memberships;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwt;
    private final AuthRateLimiter rateLimiter;
    private final JwtProperties jwtProperties;
    private final AuditService audit;

    public AuthService(UserRepository users, OrganizationRepository organizations, MembershipRepository memberships,
                       RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder, JwtTokenService jwt,
                       AuthRateLimiter rateLimiter, JwtProperties jwtProperties, AuditService audit) {
        this.users = users;
        this.organizations = organizations;
        this.memberships = memberships;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.jwt = jwt;
        this.rateLimiter = rateLimiter;
        this.jwtProperties = jwtProperties;
        this.audit = audit;
    }

    @Transactional
    public AuthResponse register(RegisterRequest input, String remoteAddress) {
        rateLimiter.check("register-ip:" + (remoteAddress == null ? "unknown" : remoteAddress), 5, java.time.Duration.ofHours(1));
        String email = input.email().trim().toLowerCase(java.util.Locale.ROOT);
        int passwordBytes = input.password().getBytes(StandardCharsets.UTF_8).length;
        if (input.password().length() < 12 || passwordBytes > 72
                || !input.password().matches("^(?=.*[A-Za-z])(?=.*\\d).*$")) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Password must be 12-72 characters and contain a letter and a number");
        }
        if (users.existsByEmail(email)) throw new ResponseStatusException(CONFLICT, "An account with this email already exists");
        UserEntity user = new UserEntity();
        user.id = UUID.randomUUID().toString();
        user.email = email;
        user.passwordHash = passwordEncoder.encode(input.password());
        user.displayName = input.displayName().trim();
        OrganizationEntity organization = new OrganizationEntity();
        organization.id = UUID.randomUUID().toString();
        organization.name = input.organizationName().trim();
        organizations.save(organization);
        users.save(user);
        MembershipEntity membership = new MembershipEntity();
        membership.id = UUID.randomUUID().toString();
        membership.organizationId = organization.id;
        membership.userId = user.id;
        membership.role = Role.OWNER.name();
        memberships.save(membership);
        audit.record(organization.id, user.id, "ACCOUNT_REGISTERED", "ORGANIZATION", organization.id);
        return issue(user, organization.id, Role.OWNER);
    }

    @Transactional
    public AuthResponse login(LoginRequest input, String remoteAddress) {
        String email = input.email().trim().toLowerCase(java.util.Locale.ROOT);
        rateLimiter.check("login-ip:" + (remoteAddress == null ? "unknown" : remoteAddress), 30, java.time.Duration.ofMinutes(15));
        rateLimiter.check("email:" + email, 10, java.time.Duration.ofMinutes(15));
        UserEntity user = users.findByEmail(email)
                .filter(candidate -> candidate.enabled && passwordEncoder.matches(input.password(), candidate.passwordHash))
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid email or password"));
        MembershipEntity membership = memberships.findFirstByUserIdOrderByCreatedAtAsc(user.id)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Account has no organization membership"));
        audit.record(membership.organizationId, user.id, "LOGIN_SUCCEEDED", "USER", user.id);
        return issue(user, membership.organizationId, Role.valueOf(membership.role));
    }

    @Transactional
    public AuthResponse switchOrganization(UserPrincipal principal, String organizationId) {
        MembershipEntity membership = memberships.findByUserIdAndOrganizationId(principal.userId(), organizationId)
                .orElseThrow(() -> new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                        "Organization membership is required"));
        UserEntity user = users.findById(principal.userId())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Account is unavailable"));
        audit.record(organizationId, user.id, "ORGANIZATION_SELECTED", "ORGANIZATION", organizationId);
        return issue(user, organizationId, Role.valueOf(membership.role));
    }

    @Transactional
    public AuthResponse refresh(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new ResponseStatusException(UNAUTHORIZED, "Invalid refresh token");
        String oldHash = hash(rawToken);
        RefreshTokenEntity old = refreshTokens.findForUpdate(oldHash)
                .filter(token -> token.revokedAt == null && token.expiresAt.isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid or expired refresh token"));
        UserEntity user = users.findById(old.userId)
                .filter(candidate -> candidate.enabled)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid refresh token"));
        MembershipEntity membership = memberships.findByUserIdAndOrganizationId(user.id, old.organizationId)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Organization membership is no longer active"));
        AuthResponse replacement = issue(user, old.organizationId, Role.valueOf(membership.role));
        old.revokedAt = LocalDateTime.now();
        old.replacedByHash = hash(replacement.refreshToken());
        refreshTokens.save(old);
        audit.record(old.organizationId, user.id, "TOKEN_REFRESHED", "USER", user.id);
        return replacement;
    }

    @Transactional
    public void revoke(String rawToken, String userId) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokens.findForUpdate(hash(rawToken))
                .filter(token -> token.userId.equals(userId) && token.revokedAt == null)
                .ifPresent(token -> {
                    token.revokedAt = LocalDateTime.now();
                    refreshTokens.save(token);
                    audit.record(token.organizationId, userId, "LOGOUT_SUCCEEDED", "USER", userId);
                });
    }

    private AuthResponse issue(UserEntity user, String organizationId, Role role) {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshTokenEntity refresh = new RefreshTokenEntity();
        refresh.tokenHash = hash(rawRefresh);
        refresh.userId = user.id;
        refresh.organizationId = organizationId;
        refresh.expiresAt = LocalDateTime.now().plus(jwtProperties.refreshTtl());
        refreshTokens.save(refresh);
        return new AuthResponse(jwt.issue(user, organizationId, role), rawRefresh, "Bearer",
                jwtProperties.accessTtl().toSeconds(), new CurrentUser(user.id, user.email, user.displayName, organizationId, role.name()));
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }

    public record RegisterRequest(
            @jakarta.validation.constraints.Email @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 254) String email,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 12, max = 72) String password,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 160) String displayName,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 160) String organizationName) {}

    public record LoginRequest(@jakarta.validation.constraints.Email @jakarta.validation.constraints.NotBlank String email,
                               @jakarta.validation.constraints.NotBlank String password) {}
    public record RefreshRequest(@jakarta.validation.constraints.NotBlank String refreshToken) {}
    public record SwitchOrganizationRequest(@jakarta.validation.constraints.NotBlank String organizationId) {}
    public record CurrentUser(String id, String email, String displayName, String organizationId, String role) {}
    public record AuthResponse(String accessToken, String refreshToken, String tokenType, long expiresIn, CurrentUser user) {}
}
