package com.TrucVanban.exchange.dto.request.receive;

import com.TrucVanban.exchange.enums.BusinessStatusCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReceiveDocumentRequest {

    @NotBlank(message = "Transaction code is required")
    private String transactionCode;

    @NotBlank(message = "Receiver code is required")
    private String receiverCode;

    @NotNull(message = "Business status code is required")
    private BusinessStatusCode businessStatusCode;
    private String statusReason;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
    private String note;
    private String changedBy;
}
