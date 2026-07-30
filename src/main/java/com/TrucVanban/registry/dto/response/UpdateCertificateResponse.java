package com.TrucVanban.registry.dto.response;

import com.TrucVanban.registry.enums.CertificateStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCertificateResponse {
    private Long certificateId;
    private CertificateStatus status;
}
