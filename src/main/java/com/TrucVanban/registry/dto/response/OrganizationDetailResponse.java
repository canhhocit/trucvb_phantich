package com.TrucVanban.registry.dto.response;

import com.TrucVanban.registry.enums.OrganizationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationDetailResponse {
    private String code;
    private String name;
    private String receiveEndpoint;
    private OrganizationStatus status;
    private ActiveCertificateDto activeCertificate;
}
