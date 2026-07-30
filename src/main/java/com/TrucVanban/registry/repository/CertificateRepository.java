package com.TrucVanban.registry.repository;

import com.TrucVanban.registry.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.TrucVanban.registry.enums.CertificateStatus;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Optional<Certificate> findByOrganizationIdAndStatus(Long organizationId, CertificateStatus status);
    Optional<Certificate> findBySerialNumberAndStatus(String serialNumber, CertificateStatus status);
    Optional<Certificate> findBySerialNumber(String serialNumber);
}
