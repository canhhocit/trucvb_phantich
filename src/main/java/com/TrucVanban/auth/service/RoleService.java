package com.TrucVanban.auth.service;

import com.TrucVanban.auth.dto.request.RoleRequest;
import com.TrucVanban.auth.dto.response.RoleDetailResponse;
import com.TrucVanban.auth.dto.response.RoleResponse;

import java.util.List;

public interface RoleService {
    RoleResponse createRole(RoleRequest request);
    RoleResponse updateRole(Long id, RoleRequest request);
    RoleDetailResponse getRoleById(Long id);
    List<RoleResponse> getAllRoles();
    void deleteRole(Long id);
}
