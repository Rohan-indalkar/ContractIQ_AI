package com.contractiq.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {
    @Id @Column(name = "token_hash", length = 64) public String tokenHash;
    @Column(name = "user_id", nullable = false, length = 36) public String userId;
    @Column(name = "organization_id", nullable = false, length = 36) public String organizationId;
    @Column(name = "expires_at", nullable = false) public LocalDateTime expiresAt;
    @Column(name = "revoked_at") public LocalDateTime revokedAt;
    @Column(name = "replaced_by_hash", length = 64) public String replacedByHash;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) public LocalDateTime createdAt;
}
