package com.TrucVanban.registry.entity;

import com.TrucVanban.registry.enums.SlaStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sla_configurations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlaConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_priority", nullable = false)
    private Integer documentPriority;

    @Column(name = "max_receive_hours", nullable = false)
    private Integer maxReceiveHours;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private SlaStatus status = SlaStatus.ACTIVE;

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
