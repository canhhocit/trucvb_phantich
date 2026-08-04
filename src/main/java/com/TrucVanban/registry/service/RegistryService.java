package com.TrucVanban.registry.service;

import com.TrucVanban.registry.dto.request.*;
import com.TrucVanban.registry.dto.response.*;
import com.TrucVanban.registry.entity.Certificate;

import java.util.List;

public interface RegistryService {

    RegisterOrganizationResponse registerOrganization(RegisterOrganizationRequest request);

    UpdateOrganizationStatusResponse updateOrganizationStatus(String code, UpdateOrganizationStatusRequest request);

    UpdateEndpointResponse updateEndpoint(String code, UpdateEndpointRequest request);

    UpdateCertificateResponse updateCertificate(String code, CertificateRequest request);

    OrganizationDetailResponse getOrganizationDetail(String code);

    UpdateSlaConfigResponse updateSlaConfig(Integer documentPriority, UpdateSlaConfigRequest request);

    Long getOrganizationIdByCode(String code);

    List<Long> getOrganizationIdsByCode(List<String> codes);

    String getOrganizationNameById(Long id);

    Integer getMaxReceiveHoursByPriority(Integer documentPriority);

    boolean checkCertificate(String signature, Long organizationId);

    Certificate findActiveCertificateBySerialNumber(String serialNumber);
}
