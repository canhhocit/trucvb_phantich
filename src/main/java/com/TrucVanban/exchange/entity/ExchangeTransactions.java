package com.TrucVanban.exchange.entity;

import com.TrucVanban.exchange.enums.SignatureStatus;
import com.TrucVanban.exchange.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_transactions")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ExchangeTransactions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_code", unique = true, nullable = false, length = 50)
    private String transactionCode;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "sender_org_id", nullable = false)
    private Long senderOrgId;

    @Column(name = "receiver_org_id", nullable = false)
    private Long receiverOrgId;

    @Column(nullable = false)
    private Integer priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", length = 30)
    private TransactionStatus currentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_status", length = 30)
    private SignatureStatus signatureStatus;

    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;

    @Version
    @Builder.Default
    @Column(name = "version")
    private Integer version = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
