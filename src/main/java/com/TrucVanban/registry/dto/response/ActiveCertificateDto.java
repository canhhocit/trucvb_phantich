package com.TrucVanban.registry.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ActiveCertificateDto {
    private String serialNumber;
    private LocalDateTime expiredAt;
}
