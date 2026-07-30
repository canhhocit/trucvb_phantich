package com.TrucVanban.exchange.repository;

import com.TrucVanban.exchange.entity.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    List<StatusHistory> findByTransactionIdOrderByCreatedAtDesc(Long transactionId);
}