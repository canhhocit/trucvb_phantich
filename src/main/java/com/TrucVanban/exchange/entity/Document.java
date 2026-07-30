package com.TrucVanban.exchange.entity;

import com.TrucVanban.exchange.enums.DocumentStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_code", unique = true, nullable = false, length = 100)
    private String documentCode;

    @Column(length = 500)
    private String title;

    @Column(columnDefinition = "text")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "extracted_metadata", columnDefinition = "jsonb")
    private JsonNode extractedMetadata;

    @Column(name = "sender_org_id", nullable = false)
    private Long senderOrgId;

    @Column(name = "document_type", length = 50)
    private String documentType;

    @Builder.Default
    @Column(name = "current_version")
    private Integer currentVersion = 1;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(length = 30)
    private DocumentStatus status = DocumentStatus.ACTIVE;

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
