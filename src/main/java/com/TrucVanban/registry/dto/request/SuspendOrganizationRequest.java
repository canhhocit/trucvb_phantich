package com.TrucVanban.registry.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuspendOrganizationRequest {

    @NotBlank(message = "Lý do khóa không được để trống")
    private String reason;
}
