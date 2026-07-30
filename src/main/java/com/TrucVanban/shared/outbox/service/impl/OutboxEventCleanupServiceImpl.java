package com.TrucVanban.shared.outbox.service.impl;

import com.TrucVanban.shared.outbox.enums.OutboxEventStatus;
import com.TrucVanban.shared.outbox.repository.OutboxEventRepository;
import com.TrucVanban.shared.outbox.service.OutboxEventCleanupService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxEventCleanupServiceImpl implements OutboxEventCleanupService {

    private static final int PROCESSED_EVENT_RETENTION_DAYS = 30;

    OutboxEventRepository outboxEventRepository;

    @Override
    @Transactional
    public void cleanupProcessedEvents() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(PROCESSED_EVENT_RETENTION_DAYS);
        long deletedCount = outboxEventRepository.deleteByStatusAndProcessedAtBefore(
                OutboxEventStatus.PROCESSED,
                cutoffTime
        );

        log.info("[outbox-cleanup] Đã xóa {} processed outbox events trước {}", deletedCount, cutoffTime);
    }
}
