package com.TrucVanban.exchange.entity;

import com.TrucVanban.exchange.enums.BusinessStatusCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_receivers")
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class DocumentReceiver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "receiver_org_id", nullable = false)
    private Long receiverOrgId;

    @Column(name = "business_status_code", nullable = false, length = 2)
    private BusinessStatusCode businessStatusCode;

    @Column(name = "status_reason", columnDefinition = "TEXT")
    private String statusReason;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Version
    @Builder.Default
    @Column(name = "version")
    private Integer version = 0;
}
