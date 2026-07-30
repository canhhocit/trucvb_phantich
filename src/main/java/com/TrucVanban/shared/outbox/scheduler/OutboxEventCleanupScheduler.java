package com.TrucVanban.shared.outbox.scheduler;

import com.TrucVanban.shared.outbox.service.OutboxEventCleanupService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxEventCleanupScheduler {

    OutboxEventCleanupService outboxEventCleanupService;

    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
    public void cleanupProcessedEvents() {
        outboxEventCleanupService.cleanupProcessedEvents();
    }
}
