package com.TrucVanban.registry.controller;

import com.TrucVanban.registry.dto.request.*;
import com.TrucVanban.registry.dto.response.*;
import com.TrucVanban.registry.service.RegistryService;
import com.TrucVanban.shared.ResponseData;

import io.swagger.v3.oas.annotations.Operation;
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
        @Operation(summary = "Đăng ký tổ chức mới")
        public ResponseEntity<ResponseData<RegisterOrganizationResponse>> registerOrganization(
                        @Valid @RequestBody RegisterOrganizationRequest request) {

                RegisterOrganizationResponse data = registryService.registerOrganization(request);

                ResponseData<RegisterOrganizationResponse> response = ResponseData
                                .<RegisterOrganizationResponse>builder()
                                .success(true)
                                .message("Đăng ký thành công. Vui lòng chờ được phê duyệt.")
                                .data(data)
                                .build();

                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        // Body: { "status": "ACTIVE|REJECTED|SUSPENDED", "reason": "..." }
        @PatchMapping("/organizations/{code}/status")
        @Operation(summary = "Cập nhật trạng thái tổ chức [phê duyệt, khóa, mở khóa]")
        public ResponseEntity<ResponseData<UpdateOrganizationStatusResponse>> updateOrganizationStatus(
                        @PathVariable String code,
                        @Valid @RequestBody UpdateOrganizationStatusRequest request) {

                UpdateOrganizationStatusResponse data = registryService.updateOrganizationStatus(code, request);

                ResponseData<UpdateOrganizationStatusResponse> response = ResponseData
                                .<UpdateOrganizationStatusResponse>builder()
                                .success(true)
                                .message("Cập nhật trạng thái tổ chức thành công")
                                .data(data)
                                .build();

                return ResponseEntity.ok(response);
        }

        @PutMapping("/organizations/{code}/endpoint")
        @Operation(summary = "Cập nhật endpoint nhận vb")
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
        @Operation(summary = "Cập nhật chứng thư số certificates")
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
        @Operation(summary = "Tra cứu tt tổ chức")
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
