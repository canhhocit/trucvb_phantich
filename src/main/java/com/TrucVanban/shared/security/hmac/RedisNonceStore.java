package com.TrucVanban.shared.security.hmac;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisNonceStore implements NonceStore {

    private final StringRedisTemplate redisTemplate;
    private static final String NONCE_KEY_PREFIX = "nonce:";

    @Override
    public boolean reserveNonce(String keyId, String nonce, Duration ttl) {
        String redisKey = NONCE_KEY_PREFIX + keyId + ":" + nonce;
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(redisKey, "1", ttl));
        } catch (DataAccessException exception) {
            log.error("[RedisNonceStore] Redis unavailable while reserving nonce {}: {}", redisKey, exception.getMessage());
            throw new HmacAuthenticationException.AuthStoreUnavailableException("Không thể kiểm tra nonce tại thời điểm này.");
        }
    }
}
