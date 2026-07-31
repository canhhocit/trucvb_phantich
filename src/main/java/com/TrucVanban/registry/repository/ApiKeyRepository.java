package com.TrucVanban.registry.repository;

import com.TrucVanban.registry.entity.ApiKey;
import com.TrucVanban.registry.enums.ApiKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyIdAndStatus(String keyId, ApiKeyStatus status);
    List<ApiKey> findByStatus(ApiKeyStatus status);
}
