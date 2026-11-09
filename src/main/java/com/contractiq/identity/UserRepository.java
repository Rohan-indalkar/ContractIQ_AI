package com.contractiq.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    java.util.List<UserEntity> findAllByIdIn(java.util.Collection<String> ids);
}
