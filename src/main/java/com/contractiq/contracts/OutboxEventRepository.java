package com.contractiq.contracts;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from OutboxEventEntity event where event.publishedAt is null order by event.createdAt")
    List<OutboxEventEntity> findPendingForUpdate(org.springframework.data.domain.Pageable pageable);
}
