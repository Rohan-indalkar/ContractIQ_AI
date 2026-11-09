package com.contractiq.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id @Column(length = 36) public String id;
    @Column(nullable = false, unique = true, length = 254) public String email;
    @Column(name = "password_hash", nullable = false, length = 100) public String passwordHash;
    @Column(name = "display_name", nullable = false, length = 160) public String displayName;
    @Column(nullable = false) public boolean enabled = true;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) public LocalDateTime createdAt;
}
