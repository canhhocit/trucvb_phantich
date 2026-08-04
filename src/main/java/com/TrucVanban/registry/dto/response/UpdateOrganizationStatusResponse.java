package com.TrucVanban.registry.dto.response;

import com.TrucVanban.registry.enums.OrganizationStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateOrganizationStatusResponse {
    private String code;
    private OrganizationStatus status;
}
