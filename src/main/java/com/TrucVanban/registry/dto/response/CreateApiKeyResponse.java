package com.TrucVanban.registry.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class CreateApiKeyResponse {

    /** keyId công khai — dùng trong header X-Api-Key */
    private String keyId;

    /**
     * Secret plaintext — chỉ trả về DUY NHẤT lần tạo này.
     * Server không lưu bản rõ, sau lần này không thể lấy lại được.
     */
    private String secret;

    /** 4 ký tự cuối của secret để đối chiếu khi hỗ trợ */
    private String secretHint;

    private Long agencyId;
    private String agencyCode;
    private String status;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
}
