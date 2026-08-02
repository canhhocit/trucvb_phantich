package com.TrucVanban.shared.security.hmac;

import com.TrucVanban.registry.entity.ApiKey;
import com.TrucVanban.registry.entity.Organization;
import com.TrucVanban.registry.enums.ApiKeyStatus;
import com.TrucVanban.registry.enums.OrganizationStatus;
import com.TrucVanban.registry.repository.ApiKeyRepository;
import com.TrucVanban.registry.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyWarmupRunner implements ApplicationRunner {

    private final ApiKeyRepository apiKeyRepository;
    private final OrganizationRepository organizationRepository;
    private final StringRedisTemplate redisTemplate;
    private final AesGcmEncryptionService encryptionService;
    private final HmacProperties hmacProperties;
    private static final String API_KEY_CACHE_PREFIX = "apikey:";
    private static final String AGENCY_KEY_SET_PREFIX = "agency:keys:";

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        if (!hmacProperties.isEnabled()) {
            return;
        }

        log.info("[ApiKeyWarmupRunner] Loading active API keys into Redis");

        // Load active API keys
        List<ApiKey> keys = apiKeyRepository.findAll().stream()
                .filter(key -> key.getStatus() == ApiKeyStatus.ACTIVE)
                .filter(key -> key.getExpiresAt() == null || OffsetDateTime.now().isBefore(key.getExpiresAt()))
                .toList();

        if (keys.isEmpty()) {
            log.info("[ApiKeyWarmupRunner] No active API keys found");
            return;
        }

        // Batch load organizations to avoid N+1 queries
        List<Long> agencyIds = keys.stream()
                .map(ApiKey::getAgencyId)
                .distinct()
                .toList();

        Map<Long, Organization> organizationMap = organizationRepository.findAllById(agencyIds).stream()
                .collect(Collectors.toMap(Organization::getId, org -> org));

        try {
            RedisCallback<Object> callback = connection -> {
                for (ApiKey apiKey : keys) {
                    Organization organization = organizationMap.get(apiKey.getAgencyId());
                    
                    // Skip if organization not found or not active
                    if (organization == null || organization.getStatus() != OrganizationStatus.ACTIVE) {
                        log.warn("[ApiKeyWarmupRunner] Skipping key {} - organization inactive or not found", apiKey.getKeyId());
                        continue;
                    }

                    String cacheKey = API_KEY_CACHE_PREFIX + apiKey.getKeyId();
                    String value = JsonUtils.toJson(new ApiKeyCacheValue(
                            apiKey.getAgencyId(),
                            organization.getCode(),
                            apiKey.getKeyId(),
                            encryptionService.decrypt(apiKey.getSecretEnc()),
                            apiKey.getStatus().name(),
                            organization.getStatus().name(),
                            apiKey.getExpiresAt()
                    ));
                    
                    connection.stringCommands().setEx(
                            cacheKey.getBytes(StandardCharsets.UTF_8),
                            hmacProperties.getCacheTtl().getSeconds(),
                            value.getBytes(StandardCharsets.UTF_8)
                    );
                    
                    connection.setCommands().sAdd(
                            (AGENCY_KEY_SET_PREFIX + apiKey.getAgencyId()).getBytes(StandardCharsets.UTF_8),
                            apiKey.getKeyId().getBytes(StandardCharsets.UTF_8)
                    );
                }
                return null;
            };

            redisTemplate.executePipelined(callback);
            log.info("[ApiKeyWarmupRunner] Warmup complete - loaded {} keys", keys.size());
        } catch (DataAccessException e) {
            log.warn("[ApiKeyWarmupRunner] Redis unavailable during warmup: {}", e.getMessage());
        }
    }
}
