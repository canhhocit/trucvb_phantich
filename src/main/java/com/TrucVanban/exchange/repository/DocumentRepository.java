package com.TrucVanban.exchange.repository;

import com.TrucVanban.exchange.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    boolean existsDocumentByDocumentCode(String documentCode);
}