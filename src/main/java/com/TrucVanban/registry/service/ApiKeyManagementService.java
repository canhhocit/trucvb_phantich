package com.TrucVanban.registry.service;

import com.TrucVanban.registry.dto.response.CreateApiKeyResponse;

import java.time.OffsetDateTime;

public interface ApiKeyManagementService {

    /**
     * Tạo API key mới cho agency.
     * Secret plaintext chỉ xuất hiện trong response này — không lưu bản rõ trong DB.
     *
     * @param agencyCode mã agency (organization.code)
     * @param expiresAt  thời điểm hết hạn, null nghĩa là không hết hạn
     */
    CreateApiKeyResponse createApiKey(String agencyCode, OffsetDateTime expiresAt);

    /**
     * Thu hồi một API key theo keyId.
     * Xoá khỏi Redis ngay lập tức sau khi revoke.
     *
     * @param keyId keyId cần thu hồi
     */
    void revokeApiKey(String keyId);
}
