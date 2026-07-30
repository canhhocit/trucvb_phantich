package com.TrucVanban.exchange.repository;

import com.TrucVanban.exchange.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByTransactionIdOrderByCreatedAtDesc(Long transactionId);

    List<AuditLog> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    List<AuditLog> findByActorIdOrderByCreatedAtDesc(String actorId);
}
