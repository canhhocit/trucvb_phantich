package com.TrucVanban.exchange.service.impl;

import com.TrucVanban.exchange.dto.request.receive.ReceiveDocumentRequest;
import com.TrucVanban.exchange.dto.request.send.ExchangeDocumentRequest;
import com.TrucVanban.exchange.dto.response.ExchangeDocumentResponse;
import com.TrucVanban.exchange.dto.response.ReceiveDocumentResponse;
import com.TrucVanban.exchange.dto.response.TransactionReceivedStatusResponse;
import com.TrucVanban.exchange.dto.response.TransactionSendStatusResponse;
import com.TrucVanban.exchange.entity.*;
import com.TrucVanban.exchange.enums.SignatureStatus;
import com.TrucVanban.exchange.enums.TransactionStatus;
import com.TrucVanban.exchange.mapper.DocumentMapper;
import com.TrucVanban.exchange.repository.*;
import com.TrucVanban.exchange.service.AuditLogService;
import com.TrucVanban.exchange.service.ExchangeService;
import com.TrucVanban.registry.service.RegistryService;
import com.TrucVanban.routing.dto.request.RoutingRequest;
import com.TrucVanban.shared.exception.DuplicateResourceException;
import com.TrucVanban.shared.exception.ForbiddenException;
import com.TrucVanban.shared.exception.ResourceNotFoundException;
import com.TrucVanban.shared.outbox.OutboxEventConstants;
import com.TrucVanban.shared.outbox.entity.OutboxEvent;
import com.TrucVanban.shared.outbox.repository.OutboxEventRepository;
import com.TrucVanban.shared.utils.NumberUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ExchangeServiceImpl implements ExchangeService {

    RegistryService registryService;

    DocumentMapper documentMapper;

    DocumentRepository documentRepository;
    ExchangeTransactionsRepository exchangeTransactionsRepository;
    DocumentVersionRepository documentVersionRepository;
    DocumentReplacementRepository documentReplacementRepository;
    DocumentReceiverRepository documentReceiverRepository;
    StatusHistoryRepository statusHistoryRepository;
    AuditLogService auditLogService;
    OutboxEventRepository outboxEventRepository;
    ObjectMapper objectMapper;

    @Override
    @Transactional
    public List<ExchangeDocumentResponse> exchangeDocument(ExchangeDocumentRequest request) {
        log.info("[exchangeDocument] Bắt đầu gửi văn bản: sender={}, receivers={}",
                request.getSenderCode(), request.getReceiverCodes());
        if (documentRepository.existsDocumentByDocumentCode(request.getDocumentCode())) {
            throw new DuplicateResourceException("Mã văn bản đã tồn tại: " + request.getDocumentCode());
        }

        Long senderId = registryService.getOrganizationIdByCode(request.getSenderCode());
        List<Long> receiverIds = registryService.getOrganizationIdsByCode(request.getReceiverCodes());

        // Lưu Document
        Document document = documentMapper.toDocument(request);
        document.setSenderOrgId(senderId);
        document = documentRepository.saveAndFlush(document);
        log.info("[exchangeDocument] Lưu document thành công: documentId={}, code={}",
                document.getId(), document.getDocumentCode());

        // Lưu DocumentVersion - dùng storagePath và payloadChecksum từ request
        // (Client đã tự upload file lên MinIO và tính SHA-256 trước khi gọi API)
        DocumentVersion existDocumentVersion = documentVersionRepository
                .findTopByDocumentIdOrderByVersionNoDesc(document.getId())
                .orElse(null);
        Integer versionNo = existDocumentVersion != null ? existDocumentVersion.getVersionNo() + 1 : 1;

        DocumentVersion documentVersion = DocumentVersion.builder()
                .documentId(document.getId())
                .versionNo(versionNo)
                .storagePath(request.getStoragePath())
                .checksum(request.getPayloadChecksum())
                .fileSize(null)  // Client không bắt buộc truyền fileSize
                .createdBy(registryService.getOrganizationNameById(senderId))
                .build();

        documentVersionRepository.save(documentVersion);
        log.info("[exchangeDocument] Lưu document version thành công: documentId={}, version={}, path={}",
                document.getId(), versionNo, request.getStoragePath());



        // Tạo ExchangeTransactions cho từng receiver
        List<ExchangeDocumentResponse> exchangeDocumentResponses = new ArrayList<>();
        final Long finalDocumentId = document.getId();
        List<ExchangeTransactions> listTransaction = new ArrayList<>();
        for (Long receiverId : receiverIds) {
            String transactionCode = "TXN-" + Year.now().getValue() + "-" + finalDocumentId + "-" + receiverId;
            Integer priority = NumberUtils.isNullOrNegative(request.getPriority()) ? 0 : request.getPriority();

            ExchangeTransactions transaction = ExchangeTransactions.builder()
                    .transactionCode(transactionCode)
                    .documentId(finalDocumentId)
                    .senderOrgId(senderId)
                    .receiverOrgId(receiverId)
                    .priority(priority)
                    // Sau khi qua filter xác minh chữ ký thành công → trạng thái VALIDATED
                    .currentStatus(TransactionStatus.VALIDATED)
                    .signatureStatus(SignatureStatus.VALID)
                    .build();
            listTransaction.add(transaction);


            exchangeDocumentResponses.add(
                    ExchangeDocumentResponse.builder()
                            .transactionCode(transactionCode)
                            .currentStatus(TransactionStatus.VALIDATED)
                            .build()
            );
        }
        listTransaction = exchangeTransactionsRepository.saveAll(listTransaction);
        outboxEventRepository.saveAll(listTransaction.stream()
                .map(this::toRoutingOutboxEvent)
                .toList());
        return exchangeDocumentResponses;
    }

    private OutboxEvent toRoutingOutboxEvent(ExchangeTransactions transaction) {
        RoutingRequest routingRequest = RoutingRequest.builder()
                .transactionCode(transaction.getTransactionCode())
                .build();

        return OutboxEvent.builder()
                .aggregateType(OutboxEventConstants.AGGREGATE_TYPE_EXCHANGE_TRANSACTION)
                .aggregateId(transaction.getId())
                .eventType(OutboxEventConstants.EVENT_TYPE_ROUTING_REQUEST)
                .payload(objectMapper.valueToTree(routingRequest))
                .build();
    }

    @Override
    @Transactional
    public ReceiveDocumentResponse ackDocument(ReceiveDocumentRequest request) {
        Long receiverId = registryService.getOrganizationIdByCode(request.getReceiverCode());
        ExchangeTransactions transaction = exchangeTransactionsRepository
                .findByTransactionCodeAndCurrentStatus(request.getTransactionCode(), TransactionStatus.DELIVERED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy giao dịch đã được luân chuyển có code: " + request.getTransactionCode()));

        if (!receiverId.equals(transaction.getReceiverOrgId())) {
            throw new ForbiddenException("Bạn không có quyền ghi nhận văn bản này");
        }

        DocumentReceiver receiver = documentMapper.toDocumentReceiver(request);
        receiver.setDocumentId(transaction.getDocumentId());
        receiver.setReceiverOrgId(receiverId);
        documentReceiverRepository.save(receiver);

        StatusHistory statusHistory = documentMapper.toStatusHistory(request);
        statusHistory.setTransactionId(transaction.getId());
        statusHistory.setActorOrgId(receiverId);
        statusHistoryRepository.save(statusHistory);

        // Ghi audit log ACK
        auditLogService.log("ACK_RECEIVED", "ORGANIZATION", request.getReceiverCode(), "SUCCESS",
                String.format("{\"transactionCode\":\"%s\",\"businessStatusCode\":\"%s\"}",
                        request.getTransactionCode(), request.getBusinessStatusCode()),
                transaction.getId(), transaction.getDocumentId());

        return ReceiveDocumentResponse.builder()
                .transactionCode(request.getTransactionCode())
                .businessStatusCode(receiver.getBusinessStatusCode())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionSendStatusResponse getTransactionStatus(String senderCode, String transactionCode) {
        Long senderId = registryService.getOrganizationIdByCode(senderCode);
        ExchangeTransactions transaction = exchangeTransactionsRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch có code: " + transactionCode));

        if (!senderId.equals(transaction.getSenderOrgId()))
            throw new ForbiddenException("Bạn không có quyền xem giao dịch này");

        return documentMapper.toTransactionStatusResponse(transaction);
    }

    @Override
    public List<TransactionReceivedStatusResponse> getTransactionReceivedStatus(String receiverCode) {
        Long receiverId = registryService.getOrganizationIdByCode(receiverCode);
        List<ExchangeTransactions> transaction = exchangeTransactionsRepository.findByReceiverOrgId(receiverId);

        if (transaction.isEmpty()) return null;

        return transaction.stream().map(t -> {
                    List<StatusHistory> statusHistories = statusHistoryRepository.findByTransactionIdOrderByCreatedAtDesc(t.getId());
                    List<TransactionReceivedStatusResponse.timeline> timelines = new ArrayList<>(statusHistories.stream()
                            .map(sh -> TransactionReceivedStatusResponse.timeline.builder()
                                    .time(sh.getCreatedAt())
                                    .status(sh.getStatusCode().getCode())
                                    .build())
                            .toList());
                    timelines.add(0, TransactionReceivedStatusResponse.timeline.builder()
                            .time(t.getCreatedAt())
                            .status(t.getCurrentStatus().name())
                            .build());
                    return TransactionReceivedStatusResponse.builder()
                            .transactionCode(t.getTransactionCode())
                            .timeline(timelines)
                            .build();
                })
                .toList();
    }
}
