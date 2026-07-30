package com.TrucVanban.exchange.mapper;

import com.TrucVanban.exchange.dto.request.receive.ReceiveDocumentRequest;
import com.TrucVanban.exchange.dto.request.send.ExchangeDocumentRequest;
import com.TrucVanban.exchange.dto.response.TransactionSendStatusResponse;
import com.TrucVanban.exchange.entity.Document;
import com.TrucVanban.exchange.entity.DocumentReceiver;
import com.TrucVanban.exchange.entity.ExchangeTransactions;
import com.TrucVanban.exchange.entity.StatusHistory;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DocumentMapper {

    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "currentVersion", constant = "1")
    @Mapping(source = "documentCode", target = "documentCode")
    @Mapping(source = "title", target = "title")
    @Mapping(source = "documentType", target = "documentType")
    @Mapping(source = "extractedMetadata", target = "extractedMetadata")
    @Mapping(source = "summary", target = "summary")
    Document toDocument(ExchangeDocumentRequest request);

    @Mapping(source = "businessStatusCode", target = "businessStatusCode")
    @Mapping(source = "statusReason", target = "statusReason")
    @Mapping(source = "processedAt", target = "processedAt")
    @Mapping(target = "documentId", ignore = true)
    @Mapping(target = "receiverOrgId", ignore = true)
    @Mapping(source = "receivedAt", target = "receivedAt")
    DocumentReceiver toDocumentReceiver(ReceiveDocumentRequest request);

    @Mapping(source = "businessStatusCode", target = "statusCode")
    @Mapping(source = "statusReason", target = "note")
    @Mapping(source = "changedBy", target = "changedBy")
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "actorOrgId", ignore = true)
    StatusHistory toStatusHistory(ReceiveDocumentRequest request);

    @Mapping(source = "transactionCode", target = "transactionCode")
    @Mapping(source = "currentStatus", target = "currentStatus")
//    @Mapping(target = "timeline", qualifiedByName = "toTimelineStatus")
    TransactionSendStatusResponse toTransactionStatusResponse(ExchangeTransactions transaction);

//    @Named("toTimelineStatus")
//    default List<TransactionSendStatusResponse.TimelineStaus> toTimelineStatus(List<StatusHistory> statusHistories) {
//        if (statusHistories == null || statusHistories.isEmpty()) return null;
//        return statusHistories.stream()
//                .map(statusHistory ->
//                    TransactionSendStatusResponse.TimelineStaus.builder()
//                            .time(statusHistory.getCreatedAt())
//                            .status(statusHistory.getStatusCode().getCode())
//                            .build()
//                        )
//                .collect(Collectors.toList());
//    }


}
