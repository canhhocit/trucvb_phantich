package com.TrucVanban.shared.outbox.service;

public interface OutboxEventCleanupService {
    void cleanupProcessedEvents();
}
