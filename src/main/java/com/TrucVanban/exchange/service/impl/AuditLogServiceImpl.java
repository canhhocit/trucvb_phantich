package com.TrucVanban.exchange.service.impl;

import com.TrucVanban.exchange.entity.AuditLog;
import com.TrucVanban.exchange.repository.AuditLogRepository;
import com.TrucVanban.exchange.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String actorType, String actorId,
                    String result, String detail,
                    Long transactionId, Long documentId) {
//            try {
//                AuditLog auditLog = AuditLog.builder()
//                        .action(action)
//                        .actorType(actorType)
//                        .actorId(actorId)
//                        .result(result)
//                        .detail(detail)
//                        .transactionId(transactionId)
//                        .documentId(documentId)
//                        .build();
//                auditLogRepository.save(auditLog);
//                log.debug("[AuditLog] Ghi nhật ký: action={}, actor={}, result={}", action, actorId, result);
//            } catch (Exception e) {
//                // Không throw exception - ghi audit log thất bại không được làm gián đoạn luồng chính
//                log.error("[AuditLog] Lỗi ghi nhật ký kiểm toán: {}", e.getMessage(), e);
//            }
    }
}
