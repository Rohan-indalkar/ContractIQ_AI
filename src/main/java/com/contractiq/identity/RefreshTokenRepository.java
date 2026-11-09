package com.contractiq.identity;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshTokenEntity token where token.tokenHash = :hash")
    Optional<RefreshTokenEntity> findForUpdate(@Param("hash") String hash);
}
