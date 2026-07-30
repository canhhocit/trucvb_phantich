package com.TrucVanban.routing.service.impl;

import com.TrucVanban.exchange.entity.Document;
import com.TrucVanban.exchange.entity.DocumentVersion;
import com.TrucVanban.exchange.entity.ExchangeTransactions;
import com.TrucVanban.exchange.enums.TransactionStatus;
import com.TrucVanban.exchange.repository.DocumentRepository;
import com.TrucVanban.exchange.repository.DocumentVersionRepository;
import com.TrucVanban.exchange.repository.ExchangeTransactionsRepository;
import com.TrucVanban.registry.entity.Organization;
import com.TrucVanban.registry.repository.OrganizationRepository;
import com.TrucVanban.routing.dto.request.RoutingRequest;
import com.TrucVanban.routing.dto.response.RoutingResponse;
import com.TrucVanban.routing.service.RoutingService;
import com.TrucVanban.shared.exception.ResourceNotFoundException;
import com.TrucVanban.shared.exception.BusinessLogicException;
import com.TrucVanban.storage.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingServiceImpl implements RoutingService {

    private final ExchangeTransactionsRepository exchangeTransactionsRepository;
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final OrganizationRepository organizationRepository;
    private final MinioService minioService;
    private final RestClient restClient;

    private record RoutingData(
            ExchangeTransactions transaction,
            Document document,
            Organization sender,
            Organization receiver,
            DocumentVersion version
    ) {}

    @Override
    @Transactional
    public RoutingResponse dispatch(RoutingRequest request) {
        validateRequest(request);
        String transactionCode = request.getTransactionCode();
        log.info("[routing] Bắt đầu điều hướng văn bản: transactionCode={}", transactionCode);

        RoutingData data = validateAndFetchData(transactionCode);

        byte[] fileContent = downloadFile(data.version());
        String fileName = buildFileName(data.document(), data.version());

        executeDispatch(data, fileContent, fileName);

        updateTransactionStatus(data.transaction());

        log.info("[routing] Điều hướng thành công: transactionCode={}", transactionCode);
        return buildResponse(data.transaction());
    }

    private void validateRequest(RoutingRequest request) {
        if (request == null || request.getTransactionCode() == null || request.getTransactionCode().isBlank()) {
            throw new BusinessLogicException("Thông tin điều hướng không hợp lệ: Thiếu transactionCode");
        }
    }

    private RoutingData validateAndFetchData(String transactionCode) {
        ExchangeTransactions transaction = exchangeTransactionsRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch: " + transactionCode));

        Document document = documentRepository.findById(transaction.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy văn bản id: " + transaction.getDocumentId()));

        Organization sender = organizationRepository.findById(transaction.getSenderOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ chức Người gửi id: " + transaction.getSenderOrgId()));

        Organization receiver = organizationRepository.findById(transaction.getReceiverOrgId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ chức Người nhận id: " + transaction.getReceiverOrgId()));

        DocumentVersion version = documentVersionRepository.findTopByDocumentIdOrderByVersionNoDesc(document.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiên bản cho văn bản id: " + document.getId()));

        return new RoutingData(transaction, document, sender, receiver, version);
    }

    private byte[] downloadFile(DocumentVersion version) {
        try {
            return minioService.download(version.getStoragePath());
        } catch (Exception e) {
            log.error("[routing] Lỗi khi tải tệp từ storage: path={}, error={}", version.getStoragePath(), e.getMessage());
            throw new BusinessLogicException("Không thể tải tệp từ bộ lưu trữ");
        }
    }

    private void executeDispatch(RoutingData data, byte[] fileContent, String fileName) {
        Organization receiver = data.receiver();
        log.info("[routing] Đang gửi tới endpoint: {}", receiver.getReceiveEndpoint());

        MultiValueMap<String, Object> body = buildMultipartBody(data, fileContent, fileName);

        ResponseEntity<String> response = restClient.post()
                .uri(receiver.getReceiveEndpoint())
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toEntity(String.class);

        validateResponse(response, data.transaction().getTransactionCode());
    }

    private void validateResponse(ResponseEntity<String> response, String transactionCode) {
        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("[routing] Gửi thất bại: code={}, status={}", transactionCode, response.getStatusCode());
            throw new BusinessLogicException("Gửi văn bản tới đơn vị nhận không thành công: " + response.getStatusCode());
        }
    }

    private void updateTransactionStatus(ExchangeTransactions transaction) {
        transaction.setCurrentStatus(TransactionStatus.DISPATCHED);
        exchangeTransactionsRepository.save(transaction);
    }

    private RoutingResponse buildResponse(ExchangeTransactions transaction) {
        return RoutingResponse.builder()
                .transactionCode(transaction.getTransactionCode())
                .currentStatus(TransactionStatus.DISPATCHED)
                .dispatchedAt(LocalDateTime.now())
                .build();
    }

    private MultiValueMap<String, Object> buildMultipartBody(
            RoutingData data,
            byte[] fileContent,
            String fileName
    ) {
        ExchangeTransactions transaction = data.transaction();
        Document document = data.document();
        Organization sender = data.sender();
        Organization receiver = data.receiver();
        DocumentVersion version = data.version();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("transactionCode", transaction.getTransactionCode());
        addIfNotNull(body, "documentCode", document.getDocumentCode());
        addIfNotNull(body, "title", document.getTitle());
        addIfNotNull(body, "summary", document.getSummary());
        addIfNotNull(body, "documentType", document.getDocumentType());
        body.add("senderCode", sender.getCode());
        addIfNotNull(body, "senderName", sender.getName());
        body.add("receiverCode", receiver.getCode());
        addIfNotNull(body, "receiverName", receiver.getName());
        body.add("priority", String.valueOf(transaction.getPriority()));
        body.add("versionNo", String.valueOf(version.getVersionNo()));

        if (document.getExtractedMetadata() != null) {
            body.add("extractedMetadata", document.getExtractedMetadata().toString());
        }

        body.add("file", new ByteArrayResource(fileContent) {
            @Override
            public String getFilename() {
                return fileName;
            }

            @Override
            public long contentLength() {
                return fileContent.length;
            }
        });
        return body;
    }

    private void addIfNotNull(MultiValueMap<String, Object> body, String key, Object value) {
        if (value != null) {
            body.add(key, value);
        }
    }

    private String buildFileName(Document document, DocumentVersion version) {
        String baseName = document.getDocumentCode();
        String storagePath = version.getStoragePath();
        String extension = storagePath.contains(".")
                ? storagePath.substring(storagePath.lastIndexOf('.'))
                : "";
        return String.format("%s-v%d%s", baseName, version.getVersionNo(), extension);
    }
}
