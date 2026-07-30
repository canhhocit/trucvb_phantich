package com.TrucVanban.exchange.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng audit_logs trong DB.
 * Ghi lại toàn bộ kết quả xác minh chữ ký và các sự kiện hệ thống.
 */
@Entity
@Table(name = "audit_logs")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "document_id")
    private Long documentId;

    /**
     * Loại hành động: SIGNATURE_VERIFIED, SIGNATURE_FAILED, REPLAY_ATTACK_DETECTED,
     * CERT_NOT_FOUND, CERT_EXPIRED, EXCHANGE_SENT, ACK_RECEIVED, ...
     */
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /**
     * Loại actor: SYSTEM, ADMIN, ORGANIZATION
     */
    @Column(name = "actor_type", length = 30)
    private String actorType;

    /**
     * ID actor: mã tổ chức, hoặc "SYSTEM"
     */
    @Column(name = "actor_id", length = 100)
    private String actorId;

    /**
     * Kết quả: SUCCESS, FAILURE
     */
    @Column(name = "result", length = 30)
    private String result;

    /**
     * Chi tiết bổ sung dạng JSON (canonical string, lý do lỗi, serial number, ...)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detail", columnDefinition = "jsonb")
    private String detail;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
