package com.TrucVanban.registry.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CertificateRequest {

    @NotBlank(message = "Public key không được để trống")
    private String publicKey;

    private String serialNumber;

    private LocalDateTime issuedAt;

    private LocalDateTime expiredAt;
}
