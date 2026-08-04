package com.TrucVanban.exchange.controller;

import com.TrucVanban.exchange.dto.request.receive.ReceiveDocumentRequest;
import com.TrucVanban.exchange.dto.request.send.ExchangeDocumentRequest;
import com.TrucVanban.exchange.dto.response.ExchangeDocumentResponse;
import com.TrucVanban.exchange.dto.response.ReceiveDocumentResponse;
import com.TrucVanban.exchange.dto.response.TransactionReceivedStatusResponse;
import com.TrucVanban.exchange.dto.response.TransactionSendStatusResponse;
import com.TrucVanban.exchange.service.ExchangeService;
import com.TrucVanban.shared.ResponseData;
import com.TrucVanban.shared.security.hmac.RequireAgencyMatch;
import com.TrucVanban.shared.utils.ListUtils;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;


    @PostMapping(value = "/exchange", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Tiếp nhận giao dịch")
    public ResponseEntity<ResponseData<List<ExchangeDocumentResponse>>> exchangeDocument(
            @RequestBody @Valid ExchangeDocumentRequest request) {
        List<ExchangeDocumentResponse> data = exchangeService.exchangeDocument(request);
        ResponseData<List<ExchangeDocumentResponse>> response = ResponseData.<List<ExchangeDocumentResponse>>builder()
                .success(true)
                .message("Tiếp nhận giao dịch thành công. Chữ ký hợp lệ - Hệ thống đang đưa vào hàng đợi ưu tiên (VALIDATED)")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/ack")
    @Operation(summary = "Ghi nhận trạng thái ACK thành công")
    public ResponseEntity<ResponseData<ReceiveDocumentResponse>> ack(
            @RequestBody @Valid ReceiveDocumentRequest request) {
        ReceiveDocumentResponse data = exchangeService.ackDocument(request);
        ResponseData<ReceiveDocumentResponse> response = ResponseData.<ReceiveDocumentResponse>builder()
                .success(true)
                .message("Ghi nhận trạng thái ACK thành công")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "{senderCode}/transactions/sended/{transactionCode}")
    @RequireAgencyMatch(pathVariable = "senderCode")
    @Operation(summary = "Lấy trạng thái giao dịch đã gửi")
    public ResponseEntity<ResponseData<TransactionSendStatusResponse>> getTransactionSendStatus(
            @PathVariable String senderCode,
            @PathVariable String transactionCode) {
        TransactionSendStatusResponse data = exchangeService.getTransactionStatus(senderCode, transactionCode);
        ResponseData<TransactionSendStatusResponse> response = ResponseData.<TransactionSendStatusResponse>builder()
                .success(true)
                .message("Hoàn thành lấy thông tin trạng thái giao dịch đã được gửi đi")
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "{receiverCode}/transactions/received")
    @RequireAgencyMatch(pathVariable = "receiverCode")
    @Operation(summary = "Lấy trạng thái giao dịch đã nhận")
    public ResponseEntity<ResponseData<?>> getTransactionReceivedStatus(@PathVariable String receiverCode) {
        List<TransactionReceivedStatusResponse> data = exchangeService.getTransactionReceivedStatus(receiverCode);

        if (ListUtils.isNullOrEmpty(data)) {
            ResponseData<?> response = ResponseData.<Object>builder()
                    .success(true)
                    .message("Không có giao dịch nào được nhận")
                    .data(null)
                    .build();
            return ResponseEntity.ok(response);
        }

        ResponseData<List<TransactionReceivedStatusResponse>> response = ResponseData.<List<TransactionReceivedStatusResponse>>builder()
                .success(true)
                .message("Hoàn thành lấy danh sách thông tin trạng thái của các giao dịch đã nhận được")
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }
}
