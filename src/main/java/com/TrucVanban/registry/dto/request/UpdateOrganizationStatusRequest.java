package com.TrucVanban.registry.dto.request;

import com.TrucVanban.registry.enums.OrganizationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateOrganizationStatusRequest {

    @NotNull(message = "Status không được để trống")
    private OrganizationStatus status;

    //khi chuyeen sang REJECTED/SUSPENDED 
    private String reason;
}
