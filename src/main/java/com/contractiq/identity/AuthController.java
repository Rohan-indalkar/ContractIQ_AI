package com.contractiq.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.contractiq.shared.AuditService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final UserRepository users;
    private final AuditService audit;

    public AuthController(AuthService auth, UserRepository users, AuditService audit) {
        this.auth = auth;
        this.users = users;
        this.audit = audit;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    AuthService.AuthResponse register(@Valid @RequestBody AuthService.RegisterRequest input, HttpServletRequest request) {
        return auth.register(input, request.getRemoteAddr());
    }

    @PostMapping("/login")
    AuthService.AuthResponse login(@Valid @RequestBody AuthService.LoginRequest input, HttpServletRequest request) {
        return auth.login(input, request.getRemoteAddr());
    }

    @PostMapping("/refresh")
    AuthService.AuthResponse refresh(@Valid @RequestBody AuthService.RefreshRequest input) {
        return auth.refresh(input.refreshToken());
    }

    @PostMapping("/switch-organization")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    AuthService.AuthResponse switchOrganization(@AuthenticationPrincipal UserPrincipal principal,
                                                 @Valid @RequestBody AuthService.SwitchOrganizationRequest input) {
        return auth.switchOrganization(principal, input.organizationId());
    }

    @PostMapping("/logout")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@Valid @RequestBody AuthService.RefreshRequest input, @AuthenticationPrincipal UserPrincipal principal) {
        auth.revoke(input.refreshToken(), principal.userId());
    }

    @GetMapping("/me")
    @Operation(security = @SecurityRequirement(name = "bearerAuth"))
    AuthService.CurrentUser me(@AuthenticationPrincipal UserPrincipal principal) {
        audit.record(principal.organizationId(), principal.userId(), "CURRENT_USER_READ", "USER", principal.userId());
        UserEntity user = users.findById(principal.userId()).orElseThrow();
        return new AuthService.CurrentUser(user.id, user.email, user.displayName, principal.organizationId(), principal.role().name());
    }
}
