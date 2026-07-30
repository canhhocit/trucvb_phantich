package com.TrucVanban.routing.dto.request;

import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingRequest {
    private String transactionCode;
}
