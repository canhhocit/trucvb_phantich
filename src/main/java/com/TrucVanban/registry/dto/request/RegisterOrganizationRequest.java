package com.TrucVanban.registry.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterOrganizationRequest {

    @NotBlank(message = "Mã tổ chức không được để trống")
    @Size(max = 35, message = "Mã tổ chức tối đa 35 ký tự")
    private String code;

    @NotBlank(message = "Tên tổ chức không được để trống")
    private String name;

    @NotBlank(message = "Endpoint nhận văn bản không được để trống")
    private String receiveEndpoint;

    @Valid
    @NotNull(message = "Thông tin chứng thư số không được để trống")
    private CertificateRequest certificate;
}
