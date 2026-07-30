package com.TrucVanban.shared.outbox.entity;

import com.TrucVanban.shared.outbox.enums.OutboxEventStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Builder.Default
    @Column(name = "event_id", nullable = false, unique = true, columnDefinition = "uuid")
    private UUID eventId = UUID.randomUUID();

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private OutboxEventStatus status = OutboxEventStatus.NEW;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        nextRetryAt = LocalDateTime.now();
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    public void markProcessed() {
        status = OutboxEventStatus.PROCESSED;
        processedAt = LocalDateTime.now();
        lastError = null;
    }

    public int getRetryCountOrDefault() {
        return retryCount == null ? 0 : retryCount;
    }

    public void markRetry(int retryCount, int delayInMinutes, String lastError) {
        this.retryCount = retryCount;
        this.nextRetryAt = LocalDateTime.now().plusMinutes(delayInMinutes);
        this.lastError = lastError;
    }

    public void markFailed(String lastError) {
        status = OutboxEventStatus.FAILED;
        this.lastError = lastError;
    }
}
