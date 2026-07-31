package com.TrucVanban.registry.service;

import com.TrucVanban.registry.dto.response.ApikeyCheckResponse;
import com.TrucVanban.registry.dto.response.CreateApiKeyResponse;

import java.time.OffsetDateTime;

public interface ApiKeyManagementService {

    /*
      agencyCode mã agency (organization.code)
      expiresAt  thời điểm hết hạn, null = voo hanj 
     */
    CreateApiKeyResponse createApiKey(String agencyCode, OffsetDateTime expiresAt);

    /*
     * Thu hồi một API key theo keyId.
     * Xoá khỏi Redis ngay lập tức sau khi revoke.
     */
    void revokeApiKey(String keyId);

    // check status apikey(chi de test xem co active k)
    ApikeyCheckResponse checkApikeyStatus(String keyId);
}
