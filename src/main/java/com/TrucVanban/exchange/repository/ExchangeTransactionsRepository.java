package com.TrucVanban.exchange.repository;

import com.TrucVanban.exchange.entity.ExchangeTransactions;
import com.TrucVanban.exchange.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

import java.util.List;
import java.util.Optional;

public interface ExchangeTransactionsRepository extends JpaRepository<ExchangeTransactions, Long> {
    Optional<ExchangeTransactions> findByTransactionCode(String transactionCode);
    Optional<ExchangeTransactions> findByTransactionCodeAndCurrentStatus(String transactionCode, TransactionStatus currentStatus);
    List<ExchangeTransactions> findByReceiverOrgId(Long receiverOrgId);

}
