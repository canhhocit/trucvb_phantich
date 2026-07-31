package com.TrucVanban.registry.controller;

import com.TrucVanban.registry.dto.response.CreateApiKeyResponse;
import com.TrucVanban.registry.service.ApiKeyManagementService;
import com.TrucVanban.shared.ResponseData;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/registry/agencies")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyManagementService apiKeyManagementService;

    /**
     * Tạo API key mới cho một agency.
     *
     * POST /api/v1/registry/agencies/{agencyCode}/api-keys
     *
     * Body param (query param tuỳ chọn):
     *   expiresAt — ISO-8601, ví dụ: 2027-01-01T00:00:00Z
     *               Nếu không truyền thì key không hết hạn.
     *
     * Response trả về secret plaintext DUY NHẤT lần này — lưu lại ngay.
     */
    @PostMapping("/{agencyCode}/api-keys")
    public ResponseEntity<ResponseData<CreateApiKeyResponse>> createApiKey(
            @PathVariable String agencyCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime expiresAt) {

        CreateApiKeyResponse data = apiKeyManagementService.createApiKey(agencyCode, expiresAt);

        ResponseData<CreateApiKeyResponse> response = ResponseData.<CreateApiKeyResponse>builder()
                .success(true)
                .message("Tạo API key thành công. Lưu lại secret ngay — sau này không thể xem lại.")
                .data(data)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Thu hồi một API key.
     *
     * DELETE /api/v1/registry/agencies/api-keys/{keyId}
     */
    @DeleteMapping("/api-keys/{keyId}")
    public ResponseEntity<ResponseData<Void>> revokeApiKey(@PathVariable String keyId) {

        apiKeyManagementService.revokeApiKey(keyId);

        ResponseData<Void> response = ResponseData.<Void>builder()
                .success(true)
                .message("Thu hồi API key thành công")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }
}
