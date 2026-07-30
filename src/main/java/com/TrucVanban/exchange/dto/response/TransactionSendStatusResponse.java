package com.TrucVanban.exchange.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TransactionSendStatusResponse {
    private String transactionCode;
    private String currentStatus;
//    private List<TimelineStaus> timeline;
//
//    @Builder
//    @Data
//    public static class TimelineStaus{
//        private LocalDateTime time;
//        private String status;
//    }
}
