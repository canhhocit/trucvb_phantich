package com.TrucVanban.routing.dto.response;

import com.TrucVanban.exchange.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingResponse {
    private String transactionCode;
    private TransactionStatus currentStatus;
    private LocalDateTime dispatchedAt;
}
