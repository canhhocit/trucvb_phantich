package com.TrucVanban.registry.service.impl;

import com.TrucVanban.registry.dto.response.ApikeyCheckResponse;
import com.TrucVanban.registry.dto.response.CreateApiKeyResponse;
import com.TrucVanban.registry.entity.ApiKey;
import com.TrucVanban.registry.entity.Organization;
import com.TrucVanban.registry.enums.ApiKeyStatus;
import com.TrucVanban.registry.enums.OrganizationStatus;
import com.TrucVanban.registry.repository.ApiKeyRepository;
import com.TrucVanban.registry.repository.OrganizationRepository;
import com.TrucVanban.registry.service.ApiKeyManagementService;
import com.TrucVanban.shared.exception.BusinessLogicException;
import com.TrucVanban.shared.exception.ResourceNotFoundException;
import com.TrucVanban.shared.security.hmac.AesGcmEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyManagementServiceImpl implements ApiKeyManagementService {

    private static final String KEY_ID_PREFIX = "tvb_live_";
    private static final int SECRET_BYTE_LENGTH = 32; // 256-bit → 43 ký tự base64url
    private static final String REDIS_API_KEY_PREFIX = "apikey:";
    private static final String REDIS_API_KEY_MISS_PREFIX = "apikey:miss:";

    private final OrganizationRepository organizationRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final AesGcmEncryptionService aesGcmEncryptionService;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public CreateApiKeyResponse createApiKey(String agencyCode, OffsetDateTime expiresAt) {
        Organization organization = organizationRepository.findByCode(agencyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ chức với mã: " + agencyCode));

        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new BusinessLogicException(
                    "Tổ chức '" + agencyCode + "' không ở trạng thái ACTIVE, không thể cấp API key");
        }

        String keyId = generateKeyId();
        String secret = generateSecret();
        String secretHint = secret.substring(secret.length() - 4);
        String secretEnc = aesGcmEncryptionService.encrypt(secret);

        ApiKey apiKey = ApiKey.builder()
                .agencyId(organization.getId())
                .keyId(keyId)
                .secretEnc(secretEnc)
                .secretHint(secretHint)
                .algorithm("HMAC_SHA256")
                .status(ApiKeyStatus.ACTIVE)
                .expiresAt(expiresAt)
                .build();

        apiKey = apiKeyRepository.save(apiKey);

        log.info("[ApiKeyManagementService] Tạo API key mới thành công: keyId={}, agencyCode={}", keyId, agencyCode);

        return CreateApiKeyResponse.builder()
                .keyId(keyId)
                .secret(secret)
                .secretHint(secretHint)
                .agencyId(organization.getId())
                .agencyCode(organization.getCode())
                .status(apiKey.getStatus().name())
                .expiresAt(apiKey.getExpiresAt())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void revokeApiKey(String keyId) {
        ApiKey apiKey = apiKeyRepository.findByKeyIdAndStatus(keyId, ApiKeyStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy API key ACTIVE với keyId: " + keyId));

        apiKey.setStatus(ApiKeyStatus.REVOKED);
        apiKey.setRevokedAt(OffsetDateTime.now());
        apiKeyRepository.save(apiKey);

        // xóa khoi redis sau trans
        evictFromRedis(keyId);

        log.info("[ApiKeyManagementService] Đã thu hồi API key: keyId={}", keyId);
    }

    // check status api
    // hàm để check lỗi, sau co the xóa 
    @Override
    public ApikeyCheckResponse checkApikeyStatus(String keyId) {
        ApiKey apiKey = apiKeyRepository.findByKeyId(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy API key với keyId: " + keyId));

        return ApikeyCheckResponse.builder()
                .keyId(apiKey.getKeyId())
                .status(apiKey.getStatus().name())
                .expiresAt(apiKey.getExpiresAt())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }

    // helper func
    private String generateKeyId() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return KEY_ID_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTE_LENGTH];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void evictFromRedis(String keyId) {
        try {
            redisTemplate.delete(REDIS_API_KEY_PREFIX + keyId);
            redisTemplate.delete(REDIS_API_KEY_MISS_PREFIX + keyId);
        } catch (DataAccessException e) {
            log.warn("[ApiKeyManagementService] Không thể xoá cache Redis cho keyId={}: {}", keyId, e.getMessage());
        }
    }
}
