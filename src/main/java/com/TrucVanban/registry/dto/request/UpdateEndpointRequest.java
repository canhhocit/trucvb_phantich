package com.TrucVanban.registry.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateEndpointRequest {
    @NotBlank(message = "Endpoint nhận văn bản không được để trống")
    private String receiveEndpoint;
}
