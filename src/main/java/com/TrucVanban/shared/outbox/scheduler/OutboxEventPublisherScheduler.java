package com.TrucVanban.shared.outbox.scheduler;

import com.TrucVanban.shared.outbox.service.OutboxEventPublisherService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OutboxEventPublisherScheduler {

    OutboxEventPublisherService outboxEventPublisherService;

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms}")
    public void publishPendingEvents() {

        outboxEventPublisherService.publishPendingEvents();
    }
}
