package com.TrucVanban.exchange.dto.response;

import com.TrucVanban.exchange.enums.BusinessStatusCode;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReceiveDocumentResponse {
    private String transactionCode;
    private BusinessStatusCode businessStatusCode;
}
