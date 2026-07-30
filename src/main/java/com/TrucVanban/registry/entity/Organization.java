package com.TrucVanban.registry.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import com.TrucVanban.registry.enums.OrganizationStatus;

@Entity
@Table(name = "organizations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 35)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "receive_endpoint", nullable = false, length = 500)
    private String receiveEndpoint;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private OrganizationStatus status = OrganizationStatus.PENDING_APPROVAL;

    @Column(name = "reject_reason")
    private String rejectReason;

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
