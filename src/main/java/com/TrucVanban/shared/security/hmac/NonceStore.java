package com.TrucVanban.shared.security.hmac;

import java.time.Duration;

public interface NonceStore {
    boolean reserveNonce(String keyId, String nonce, Duration ttl);
}
