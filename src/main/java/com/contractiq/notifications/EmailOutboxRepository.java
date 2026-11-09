package com.contractiq.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import jakarta.persistence.LockModeType;

public interface EmailOutboxRepository extends JpaRepository<EmailOutboxEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EmailOutboxEntity e where e.sentAt is null and e.attemptCount < :maxAttempts order by e.createdAt")
    List<EmailOutboxEntity> findPending(@Param("maxAttempts") int maxAttempts, Pageable pageable);
}
