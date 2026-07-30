package com.TrucVanban.registry.dto.request;

import lombok.Data;

@Data
public class ApproveOrganizationRequest {

    private Boolean isApproved;

    private String rejectReason;
}
