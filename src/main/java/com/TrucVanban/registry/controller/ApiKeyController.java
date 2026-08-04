package com.TrucVanban.registry.controller;

import com.TrucVanban.registry.dto.response.ApikeyCheckResponse;
import com.TrucVanban.registry.dto.response.CreateApiKeyResponse;
import com.TrucVanban.registry.service.ApiKeyManagementService;
import com.TrucVanban.shared.ResponseData;

import io.swagger.v3.oas.annotations.Operation;
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

    @PostMapping("/{agencyCode}/api-keys")
    @Operation(summary = "tạo apikey ")
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

    @DeleteMapping("/api-keys/{keyId}")
    @Operation(summary = "thu hồi apikey")
    public ResponseEntity<ResponseData<Void>> revokeApiKey(@PathVariable String keyId) {

        apiKeyManagementService.revokeApiKey(keyId);

        ResponseData<Void> response = ResponseData.<Void>builder()
                .success(true)
                .message("Thu hồi API key thành công")
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api-keys/{keyId}")
    @Operation(summary = "check API key [đang để test, có thể xóa endpoint này]")
    public ResponseEntity<ResponseData<ApikeyCheckResponse>> getApiKeyStatus(@PathVariable String keyId) {
        ApikeyCheckResponse data = apiKeyManagementService.checkApikeyStatus(keyId);
        ResponseData<ApikeyCheckResponse> response = ResponseData.<ApikeyCheckResponse>builder()
                .success(true)
                .message("check API key")
                .data(data)
                .build();
        return ResponseEntity.ok(response);
    }
}
