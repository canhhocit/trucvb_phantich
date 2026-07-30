package com.TrucVanban.exchange.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_versions")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DocumentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "update_reason", columnDefinition = "text")
    private String updateReason;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
