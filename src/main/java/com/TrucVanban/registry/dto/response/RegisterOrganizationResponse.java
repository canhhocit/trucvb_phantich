package com.TrucVanban.registry.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.TrucVanban.registry.enums.OrganizationStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterOrganizationResponse {

    private Long organizationId;

    private String code;

    private OrganizationStatus status;
}
