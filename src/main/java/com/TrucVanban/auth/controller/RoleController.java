package com.TrucVanban.auth.controller;

import com.TrucVanban.auth.dto.request.RoleRequest;
import com.TrucVanban.auth.dto.response.RoleDetailResponse;
import com.TrucVanban.auth.dto.response.RoleResponse;
import com.TrucVanban.auth.service.RoleService;
import com.TrucVanban.shared.ResponseData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ResponseEntity<ResponseData<RoleResponse>> createRole(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseData.<RoleResponse>builder()
                        .message("Tạo vai trò thành công")
                        .data(roleService.createRole(request))
                        .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseData<RoleResponse>> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(ResponseData.<RoleResponse>builder()
                .message("Cập nhật vai trò thành công")
                .data(roleService.updateRole(id, request))
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<RoleDetailResponse>> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(ResponseData.<RoleDetailResponse>builder()
                .message("Lấy thông tin vai trò thành công")
                .data(roleService.getRoleById(id))
                .build());
    }

    @GetMapping
    public ResponseEntity<ResponseData<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ResponseData.<List<RoleResponse>>builder()
                .message("Lấy danh sách vai trò thành công")
                .data(roleService.getAllRoles())
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<Void>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ResponseData.<Void>builder()
                .message("Xóa vai trò thành công")
                .build());
    }
}
