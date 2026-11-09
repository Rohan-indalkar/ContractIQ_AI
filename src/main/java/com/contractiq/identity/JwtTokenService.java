package com.contractiq.identity;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtTokenService {
    private final JwtProperties properties;
    private SecretKey key;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validateConfiguration() {
        if (properties.secret() == null || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must be configured with at least 32 bytes");
        }
        key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issue(UserEntity user, String organizationId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .audience().add(properties.audience()).and()
                .subject(user.id)
                .claim("org", organizationId)
                .claim("email", user.email)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTtl())))
                .signWith(key)
                .compact();
    }

    public UserPrincipal verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.issuer())
                .requireAudience(properties.audience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new UserPrincipal(claims.getSubject(), claims.get("org", String.class),
                claims.get("email", String.class), Role.valueOf(claims.get("role", String.class)));
    }
}
