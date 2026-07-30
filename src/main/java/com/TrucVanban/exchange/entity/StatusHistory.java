package com.TrucVanban.exchange.entity;

import com.TrucVanban.exchange.enums.BusinessStatusCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "status_histories")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class StatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private Long transactionId;

    @Column(name = "actor_org_id")
    private Long actorOrgId;

    @Column(name = "status_code", nullable = false, length = 2)
    private BusinessStatusCode statusCode;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
