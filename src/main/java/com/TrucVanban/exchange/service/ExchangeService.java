package com.TrucVanban.exchange.service;

import com.TrucVanban.exchange.dto.request.receive.ReceiveDocumentRequest;
import com.TrucVanban.exchange.dto.request.send.ExchangeDocumentRequest;
import com.TrucVanban.exchange.dto.response.ExchangeDocumentResponse;
import com.TrucVanban.exchange.dto.response.ReceiveDocumentResponse;
import com.TrucVanban.exchange.dto.response.TransactionReceivedStatusResponse;
import com.TrucVanban.exchange.dto.response.TransactionSendStatusResponse;

import java.util.List;

public interface ExchangeService {
    List<ExchangeDocumentResponse> exchangeDocument(ExchangeDocumentRequest request);
    ReceiveDocumentResponse ackDocument(ReceiveDocumentRequest request);
    TransactionSendStatusResponse getTransactionStatus(String senderCode, String transactionCode);
    List<TransactionReceivedStatusResponse> getTransactionReceivedStatus(String receiverCode);
}
