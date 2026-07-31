package com.TrucVanban.registry.dto.response;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;


@Data
@Builder
public class ApikeyCheckResponse {
    private String keyId;
    private String status;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
}
