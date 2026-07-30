package com.TrucVanban.exchange.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TransactionReceivedStatusResponse {
    private String transactionCode;
    private List<timeline> timeline;

    @Data
    @Builder
    public static class timeline{
        private LocalDateTime time;
        private String status;
    }
}
