package com.TrucVanban.exchange.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_replacements")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentReplacement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "replacement_document_id", nullable = false)
    private Long replacementDocumentId;

    @Column(name = "replaced_document_id", nullable = false)
    private Long replacedDocumentId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
