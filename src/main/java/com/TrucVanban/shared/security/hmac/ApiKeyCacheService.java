package com.TrucVanban.shared.security.hmac;

import com.TrucVanban.registry.entity.ApiKey;
import com.TrucVanban.registry.enums.ApiKeyStatus;
import com.TrucVanban.registry.repository.ApiKeyRepository;
import com.TrucVanban.registry.repository.OrganizationRepository;
import com.TrucVanban.registry.entity.Organization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyCacheService {

    private static final String API_KEY_CACHE_PREFIX = "apikey:";
    private static final String API_KEY_MISS_PREFIX = "apikey:miss:";
    private static final String AGENCY_KEY_SET_PREFIX = "agency:keys:";

    private final ApiKeyRepository apiKeyRepository;
    private final OrganizationRepository organizationRepository;
    private final StringRedisTemplate redisTemplate;
    private final AesGcmEncryptionService encryptionService;
    private final HmacProperties hmacProperties;

    public ApiKeyCacheValue getApiKey(String keyId) {
        String cacheKey = API_KEY_CACHE_PREFIX + keyId;
        try {
            String negativeMarker = redisTemplate.opsForValue().get(API_KEY_MISS_PREFIX + keyId);
            if (negativeMarker != null) {
                return null;
            }
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                return JsonUtils.fromJson(json, ApiKeyCacheValue.class);
            }
        } catch (DataAccessException e) {
            log.warn("[ApiKeyCacheService] redis lỗi khi kiểm tra API key: {}", e.getMessage());
        }

        return loadApiKeyFromDatabase(keyId);
    }

    private ApiKeyCacheValue loadApiKeyFromDatabase(String keyId) {
        try {
            Optional<ApiKey> apiKeyOptional = apiKeyRepository.findByKeyIdAndStatus(keyId, ApiKeyStatus.ACTIVE);
            if (apiKeyOptional.isEmpty()) {
                negativeCacheMiss(keyId);
                return null;
            }
            ApiKey apiKey = apiKeyOptional.get();
            if (apiKey.getExpiresAt() != null && OffsetDateTime.now().isAfter(apiKey.getExpiresAt())) {
                log.warn("[ApiKeyCacheService] API key đã hết hạn trong DB: {}", keyId);
                negativeCacheMiss(keyId);
                return null;
            }

            Organization organization = organizationRepository.findById(apiKey.getAgencyId()).orElse(null);
            if (organization == null || organization.getStatus() != com.TrucVanban.registry.enums.OrganizationStatus.ACTIVE) {
                return null;
            }

            String secret = encryptionService.decrypt(apiKey.getSecretEnc());
            ApiKeyCacheValue cacheValue = new ApiKeyCacheValue(
                    apiKey.getAgencyId(),
                    organization.getCode(),
                    apiKey.getKeyId(),
                    secret,
                    apiKey.getStatus().name(),
                    organization.getStatus().name(),
                    apiKey.getExpiresAt()
            );
            cacheApiKey(cacheValue);
            return cacheValue;
        } catch (DataAccessException e) {
            log.warn("[ApiKeyCacheService] redis lôix khi làm mới cache API key: {}", e.getMessage());
            return findApiKeyFromDb(keyId);
        }
    }

    private void cacheApiKey(ApiKeyCacheValue value) {
        String cacheKey = API_KEY_CACHE_PREFIX + value.keyId();
        try {
            redisTemplate.opsForValue().set(cacheKey, JsonUtils.toJson(value), hmacProperties.getCacheTtl());
            redisTemplate.opsForSet().add(AGENCY_KEY_SET_PREFIX + value.agencyId(), value.keyId());
        } catch (DataAccessException e) {
            log.warn("[ApiKeyCacheService] Không thể ghi cache API key hoặc tập hợp cơ quan: {}", e.getMessage());
        }
    }

    private void negativeCacheMiss(String keyId) {
        try {
            redisTemplate.opsForValue().set(API_KEY_MISS_PREFIX + keyId, "1", hmacProperties.getNegativeCacheTtl());
        } catch (DataAccessException e) {
            log.warn("[ApiKeyCacheService] Không thể ghi negative cache cho trường hợp API key: {}", e.getMessage());
        }
    }

    private ApiKeyCacheValue findApiKeyFromDb(String keyId) {
        return apiKeyRepository.findByKeyIdAndStatus(keyId, ApiKeyStatus.ACTIVE)
                .flatMap(apiKey -> organizationRepository.findById(apiKey.getAgencyId())
                        .filter(org -> org.getStatus() == com.TrucVanban.registry.enums.OrganizationStatus.ACTIVE)
                        .map(org -> new ApiKeyCacheValue(
                                apiKey.getAgencyId(),
                                org.getCode(),
                                apiKey.getKeyId(),
                                encryptionService.decrypt(apiKey.getSecretEnc()),
                                apiKey.getStatus().name(),
                                org.getStatus().name(),
                                apiKey.getExpiresAt()
                        )))
                .orElse(null);
    }

    // xóa all API key cache 
    public void evictAgencyCache(Long agencyId) {
        try {
            String agencyKeySetKey = AGENCY_KEY_SET_PREFIX + agencyId;
            var keyIds = redisTemplate.opsForSet().members(agencyKeySetKey);
            
            if (keyIds == null || keyIds.isEmpty()) {
                log.debug("[ApiKeyCacheService] Không tìm thấy API key nào được cache cho cơ quan {}", agencyId);
                return;
            }

            for (String keyId : keyIds) {
                redisTemplate.delete(API_KEY_CACHE_PREFIX + keyId);
                redisTemplate.delete(API_KEY_MISS_PREFIX + keyId);
            }
            
            redisTemplate.delete(agencyKeySetKey);
            
            log.info("[ApiKeyCacheService] Đã xóa {} cache API key cho cơ quan {}", keyIds.size(), agencyId);
        } catch (DataAccessException e) {
            log.warn("[ApiKeyCacheService] xóa cache cho cơ quan {} thất bại: {}", agencyId, e.getMessage());
        }
    }
}
