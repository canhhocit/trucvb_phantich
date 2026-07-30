package com.TrucVanban.registry.repository;

import com.TrucVanban.registry.entity.SlaConfiguration;
import com.TrucVanban.registry.enums.SlaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlaConfigurationRepository extends JpaRepository<SlaConfiguration, Long> {

    Optional<SlaConfiguration> findByDocumentPriority(Integer documentPriority);

    Optional<SlaConfiguration> findByDocumentPriorityAndStatus(Integer documentPriority, SlaStatus status);
}
