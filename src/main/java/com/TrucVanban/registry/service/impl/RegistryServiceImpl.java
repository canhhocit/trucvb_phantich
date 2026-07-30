package com.TrucVanban.registry.service.impl;

import com.TrucVanban.registry.dto.request.*;
import com.TrucVanban.registry.dto.response.*;
import com.TrucVanban.registry.entity.Certificate;
import com.TrucVanban.registry.entity.Organization;
import com.TrucVanban.registry.entity.SlaConfiguration;
import com.TrucVanban.registry.enums.CertificateStatus;
import com.TrucVanban.registry.enums.OrganizationStatus;
import com.TrucVanban.registry.enums.SlaStatus;
import com.TrucVanban.registry.mapper.OrganizationMapper;
import com.TrucVanban.registry.mapper.SlaConfigMapper;
import com.TrucVanban.registry.repository.CertificateRepository;
import com.TrucVanban.registry.repository.OrganizationRepository;
import com.TrucVanban.registry.repository.SlaConfigurationRepository;
import com.TrucVanban.registry.service.RegistryService;
import com.TrucVanban.shared.exception.BusinessLogicException;
import com.TrucVanban.shared.exception.DuplicateResourceException;
import com.TrucVanban.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryServiceImpl implements RegistryService {

    private final OrganizationRepository organizationRepository;
    private final CertificateRepository certificateRepository;
    private final SlaConfigurationRepository slaConfigurationRepository;
    private final OrganizationMapper organizationMapper;
    private final SlaConfigMapper slaConfigMapper;

    @Override
    @Transactional
    public RegisterOrganizationResponse registerOrganization(RegisterOrganizationRequest request) {
        if (organizationRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Mã tổ chức '" + request.getCode() + "' đã tồn tại trong hệ thống");
        }

        Organization organization = organizationMapper.toEntity(request);
        organization = organizationRepository.save(organization);

        Certificate certificate = organizationMapper.toCertificateEntity(request.getCertificate());
        certificate.setOrganizationId(organization.getId());
        certificateRepository.save(certificate);

        log.info("Đăng ký tổ chức - chờ phê duyệt: code={}, id={}", organization.getCode(), organization.getId());

        return organizationMapper.toRegisterResponse(organization);
    }

    @Override
    @Transactional
    public SuspendOrganizationResponse suspendOrganization(String code, SuspendOrganizationRequest request) {
        Organization organization = organizationRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ chức với mã: " + code));

        if (organization.getStatus() == OrganizationStatus.SUSPENDED) {
            throw new BusinessLogicException("Tổ chức '" + code + "' đã bị khóa trước đó");
        }

        organization.setStatus(OrganizationStatus.SUSPENDED);
        organizationRepository.save(organization);

        log.info("Khóa tổ chức thành công: code={}, reason={}", code, request.getReason());

        return organizationMapper.toSuspendResponse(organization);
    }

    @Override
    @Transactional
    public ApproveOrganizationResponse approveOrganization(String code, ApproveOrganizationRequest request) {
        Organization organization = organizationRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ chức với mã: " + code));

        if (organization.getStatus() != OrganizationStatus.PENDING_APPROVAL) {
            throw new BusinessLogicException("Tổ chức '" + code + "' không ở trạng thái chờ duyệt (hiện tại: " + organization.getStatus() + ")");
        }

        if (Boolean.TRUE.equals(request.getIsApproved())) {
            organization.setStatus(OrganizationStatus.ACTIVE);
            organization.setRejectReason(null);
            log.info("Phê duyệt tổ chức thành công: code={}", code);
        } else {
            organization.setStatus(OrganizationStatus.REJECTED);
            organization.setRejectReason(request.getRejectReason());
            log.info("Từ chối tổ chức: code={}, reason={}", code, request.getRejectReason());
        }

        organizationRepository.save(organization);

        return ApproveOrganizationResponse.builder()
                .code(organization.getCode())
                .status(organization.getStatus())
                .build();
    }

    @Override
    @Transactional
    public UpdateEndpointResponse updateEndpoint(String code, UpdateEndpointRequest request) {
        Organization organization = organizationRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ chức với mã: " + code));

        organization.setReceiveEndpoint(request.getReceiveEndpoint());
        organizationRepository.save(organization);

        log.info("Cập nhật endpoint thành công: code={}, new_endpoint={}", code, request.getReceiveEndpoint());
        return organizationMapper.toUpdateEndpointResponse(organization);
    }

    @Override
    @Transactional
    public UpdateCertificateResponse updateCertificate(String code, CertificateRequest request) {
        Organization organization = organizationRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ chức với mã: " + code));

        // cũ E -> hủy
        certificateRepository.findByOrganizationIdAndStatus(organization.getId(), CertificateStatus.ACTIVE)
                .ifPresent(cert -> {
                    cert.setStatus(CertificateStatus.EXPIRED);
                    certificateRepository.save(cert);
                });

        Certificate newCert = organizationMapper.toCertificateEntity(request);
        newCert.setOrganizationId(organization.getId());
        newCert.setStatus(CertificateStatus.ACTIVE);
        newCert = certificateRepository.save(newCert);

        log.info("Cập nhật chứng thư số thành công: code={}, cert_id={}", code, newCert.getId());
        return new UpdateCertificateResponse(newCert.getId(), newCert.getStatus());
    }

    @Override
    public OrganizationDetailResponse getOrganizationDetail(String code) {
        Organization organization = organizationRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ chức với mã: " + code));

        OrganizationDetailResponse response = organizationMapper.toOrganizationDetailResponse(organization);

        certificateRepository.findByOrganizationIdAndStatus(organization.getId(), CertificateStatus.ACTIVE)
                .ifPresent(cert -> {
                    response.setActiveCertificate(organizationMapper.toActiveCertificateDto(cert));
                });

        return response;
    }

    @Override
    @Transactional
    public UpdateSlaConfigResponse updateSlaConfig(Integer documentPriority, UpdateSlaConfigRequest request) {
        SlaConfiguration slaConfig = slaConfigurationRepository.findByDocumentPriority(documentPriority)
                .orElseGet(() -> {
                    SlaConfiguration newSla = new SlaConfiguration();
                    newSla.setDocumentPriority(documentPriority);
                    return newSla;
                });

        slaConfig.setMaxReceiveHours(request.getMaxReceiveHours());
        slaConfig = slaConfigurationRepository.save(slaConfig);

        log.info("Cập nhật SLA thành công: priority={}, maxHours={}", documentPriority, request.getMaxReceiveHours());
        return slaConfigMapper.toResponse(slaConfig);
    }

    //not for api
    @Override
    public Long getOrganizationIdByCode(String code) {
        Organization organization = organizationRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ chức với mã: " + code));
        return organization.getId();
    }

    @Override
    public List<Long> getOrganizationIdsByCode(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }

        Map<String, Long> organizationIdsByCode = organizationRepository.findByCodeIn(codes).stream()
                .collect(LinkedHashMap::new, (map, organization) -> map.put(organization.getCode(), organization.getId()), Map::putAll);

        List<String> missingCodes = codes.stream()
                .distinct()
                .filter(code -> !organizationIdsByCode.containsKey(code))
                .toList();

        if (!missingCodes.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy tổ chức với mã: " + String.join(", ", missingCodes));
        }

        return codes.stream()
                .map(organizationIdsByCode::get)
                .toList();
    }

    @Override
    public String getOrganizationNameById(Long id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tổ chức với id: " + id));
        return organization.getName();
    }

    @Override
    public boolean checkCertificate(String signature, Long organizationId) {
        return certificateRepository.findByOrganizationIdAndStatus(organizationId, CertificateStatus.ACTIVE)
                .map(cert -> !LocalDateTime.now().isAfter(cert.getExpiredAt()) && cert.getPublicKey().equals(signature))
                .orElse(false);
    }

    @Override
    public Integer getMaxReceiveHoursByPriority(Integer documentPriority) {
        return slaConfigurationRepository.findByDocumentPriorityAndStatus(documentPriority, SlaStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cấu hình SLA cho ưu tiên: " + documentPriority))
                .getMaxReceiveHours();
    }

    @Override
    @Cacheable(value = "certificateCache", key = "#serialNumber", unless = "#result == null")
    public Certificate findActiveCertificateBySerialNumber(String serialNumber) {
        return certificateRepository.findBySerialNumberAndStatus(serialNumber, CertificateStatus.ACTIVE)
                .filter(cert -> cert.getExpiredAt() != null && !LocalDateTime.now().isAfter(cert.getExpiredAt()))
                .orElse(null);
    }
}
