package com.TrucVanban.registry.mapper;

import com.TrucVanban.registry.dto.request.CertificateRequest;
import com.TrucVanban.registry.dto.request.RegisterOrganizationRequest;
import com.TrucVanban.registry.dto.response.*;
import com.TrucVanban.registry.entity.Certificate;
import com.TrucVanban.registry.entity.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "rejectReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Organization toEntity(RegisterOrganizationRequest request);

    @Mapping(source = "id", target = "organizationId")
    RegisterOrganizationResponse toRegisterResponse(Organization organization);

    SuspendOrganizationResponse toSuspendResponse(Organization organization);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organizationId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Certificate toCertificateEntity(CertificateRequest request);

    UpdateEndpointResponse toUpdateEndpointResponse(Organization organization);

    ActiveCertificateDto toActiveCertificateDto(Certificate certificate);

    @Mapping(target = "activeCertificate", ignore = true)
    OrganizationDetailResponse toOrganizationDetailResponse(Organization organization);
}
