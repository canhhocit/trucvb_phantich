package com.TrucVanban.exchange.service;

/**
 * Service ghi nhật ký kiểm toán (Audit Log) cho các sự kiện xác minh chữ ký,
 * giao dịch văn bản, và các sự kiện hệ thống quan trọng.
 */
public interface AuditLogService {
    void log(String action, String actorType, String actorId,
             String result, String detail,
             Long transactionId, Long documentId);
}
