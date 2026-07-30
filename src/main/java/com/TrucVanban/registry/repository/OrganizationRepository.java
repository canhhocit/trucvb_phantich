package com.TrucVanban.registry.repository;

import com.TrucVanban.registry.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    Optional<Organization> findByCode(String code);

    List<Organization> findByCodeIn(List<String> codes);

    boolean existsByCode(String code);
}
