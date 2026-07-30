package com.TrucVanban.registry.controller;

import com.TrucVanban.registry.dto.request.*;
import com.TrucVanban.registry.dto.response.*;
import com.TrucVanban.registry.service.RegistryService;
import com.TrucVanban.shared.ResponseData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/registry")
@RequiredArgsConstructor
public class RegistryController {

    private final RegistryService registryService;

    @PostMapping("/organizations")
    public ResponseEntity<ResponseData<RegisterOrganizationResponse>> registerOrganization(
            @Valid @RequestBody RegisterOrganizationRequest request) {

        RegisterOrganizationResponse data = registryService.registerOrganization(request);

        ResponseData<RegisterOrganizationResponse> response = ResponseData.<RegisterOrganizationResponse>builder()
                .success(true)
                .message("Đăng ký thành công. Vui lòng chờ được phê duyệt.")
                .data(data)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/organizations/{code}/approve")
    public ResponseEntity<ResponseData<ApproveOrganizationResponse>> approveOrganization(
            @PathVariable String code,
            @Valid @RequestBody ApproveOrganizationRequest request) {

        ApproveOrganizationResponse data = registryService.approveOrganization(code, request);

        String message = data.getStatus() == com.TrucVanban.registry.enums.OrganizationStatus.ACTIVE
                ? "Đã phê duyệt tổ chức thành công"
                : "Đã từ chối yêu cầu đăng ký của tổ chức";

        ResponseData<ApproveOrganizationResponse> response = ResponseData.<ApproveOrganizationResponse>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/organizations/{code}/suspend")
    public ResponseEntity<ResponseData<SuspendOrganizationResponse>> suspendOrganization(
            @PathVariable String code,
            @Valid @RequestBody SuspendOrganizationRequest request) {

        SuspendOrganizationResponse data = registryService.suspendOrganization(code, request);

        ResponseData<SuspendOrganizationResponse> response = ResponseData.<SuspendOrganizationResponse>builder()
                .success(true)
                .message("Khóa tổ chức thành công")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    @PutMapping("/organizations/{code}/endpoint")
    public ResponseEntity<ResponseData<UpdateEndpointResponse>> updateEndpoint(
            @PathVariable String code,
            @Valid @RequestBody UpdateEndpointRequest request) {

        UpdateEndpointResponse data = registryService.updateEndpoint(code, request);

        ResponseData<UpdateEndpointResponse> response = ResponseData.<UpdateEndpointResponse>builder()
                .success(true)
                .message("Cập nhật endpoint thành công")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/organizations/{code}/certificates")
    public ResponseEntity<ResponseData<UpdateCertificateResponse>> updateCertificate(
            @PathVariable String code,
            @Valid @RequestBody CertificateRequest request) {

        UpdateCertificateResponse data = registryService.updateCertificate(code, request);

        ResponseData<UpdateCertificateResponse> response = ResponseData.<UpdateCertificateResponse>builder()
                .success(true)
                .message("Cập nhật chứng thư số thành công")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/organizations/{code}")
    public ResponseEntity<ResponseData<OrganizationDetailResponse>> getOrganizationDetail(
            @PathVariable String code) {

        OrganizationDetailResponse data = registryService.getOrganizationDetail(code);

        ResponseData<OrganizationDetailResponse> response = ResponseData.<OrganizationDetailResponse>builder()
                .success(true)
                .message("Tra cứu thông tin thành công")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }
}
