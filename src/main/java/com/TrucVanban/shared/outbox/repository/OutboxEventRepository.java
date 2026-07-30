package com.TrucVanban.shared.outbox.repository;

import com.TrucVanban.shared.outbox.entity.OutboxEvent;
import com.TrucVanban.shared.outbox.enums.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    @Query(value = """
           SELECT *
           FROM outbox_event
           WHERE status = :status
           AND next_retry_at <= NOW()
           ORDER BY next_retry_at
           LIMIT 50
           FOR UPDATE SKIP LOCKED
           """, nativeQuery = true)
    List<OutboxEvent> findAndLockEvent(String status);

    long deleteByStatusAndProcessedAtBefore(OutboxEventStatus status, LocalDateTime processedAt);
}
