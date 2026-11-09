package com.contractiq.identity;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {
    private static final String SECRET = "test-only-signing-key-with-at-least-32-bytes";

    @Test
    void tokenClaimsAreSignedAndRecoverTenantRole() {
        JwtTokenService service = service("contractiq", "contractiq-api");
        UserEntity user = new UserEntity();
        user.id = "user-a";
        user.email = "a@example.test";

        UserPrincipal principal = service.verify(service.issue(user, "org-a", Role.ADMIN));

        assertEquals("user-a", principal.userId());
        assertEquals("org-a", principal.organizationId());
        assertEquals("a@example.test", principal.email());
        assertEquals(Role.ADMIN, principal.role());
    }

    @Test
    void rejectsTamperedTokenAndWrongIssuerConfiguration() {
        JwtTokenService service = service("contractiq", "contractiq-api");
        UserEntity user = new UserEntity();
        user.id = "user-a";
        user.email = "a@example.test";
        String token = service.issue(user, "org-a", Role.MEMBER);

        assertThrows(io.jsonwebtoken.JwtException.class,
                () -> service.verify(token.substring(0, token.length() - 2) + "xx"));
        assertThrows(io.jsonwebtoken.JwtException.class,
                () -> service("other-issuer", "contractiq-api").verify(token));
    }

    private JwtTokenService service(String issuer, String audience) {
        JwtTokenService service = new JwtTokenService(new JwtProperties(issuer, audience, SECRET,
                Duration.ofMinutes(10), Duration.ofDays(30)));
        service.validateConfiguration();
        return service;
    }
}
