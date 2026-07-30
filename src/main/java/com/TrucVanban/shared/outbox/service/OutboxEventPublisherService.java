package com.TrucVanban.shared.outbox.service;

public interface OutboxEventPublisherService {
    void publishPendingEvents();
}
