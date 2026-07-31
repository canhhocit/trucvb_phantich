package com.TrucVanban.shared.security.hmac;

import java.time.OffsetDateTime;

public record ApiKeyCacheValue(
        Long agencyId,
        String agencyCode,
        String keyId,
        String secret,
        String keyStatus,
        String agencyStatus,
        OffsetDateTime expiresAt
) {
}
