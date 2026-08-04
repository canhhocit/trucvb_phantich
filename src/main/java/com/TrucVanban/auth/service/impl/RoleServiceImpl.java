package com.TrucVanban.auth.service.impl;

import com.TrucVanban.auth.dto.request.RoleRequest;
import com.TrucVanban.auth.dto.response.RoleDetailResponse;
import com.TrucVanban.auth.dto.response.RoleResponse;
import com.TrucVanban.auth.entity.Permission;
import com.TrucVanban.auth.entity.Role;
import com.TrucVanban.auth.mapper.RoleMapper;
import com.TrucVanban.auth.repository.PermissionRepository;
import com.TrucVanban.auth.repository.RoleRepository;
import com.TrucVanban.auth.service.RoleService;
import com.TrucVanban.shared.exception.DuplicateResourceException;
import com.TrucVanban.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    @Override
    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.findByCode(request.getCode()).isPresent()) {
            throw new DuplicateResourceException("Mã vai trò đã tồn tại");
        }
        Role role = roleMapper.toEntity(request);
        setPermissions(role, request.getPermissions());
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponse updateRole(Long id, RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò"));
        
        if (request.getCode() != null && !request.getCode().equals(role.getCode())) {
            if (roleRepository.findByCode(request.getCode()).isPresent()) {
                throw new DuplicateResourceException("Mã vai trò đã tồn tại");
            }
        }

        roleMapper.updateEntityFromRequest(request, role);

        //request.getPermissions() = null -> giữ nguyên db
        if (request.getPermissions() != null) {
            setPermissions(role, request.getPermissions());
        }
        
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional(readOnly = true)
    public RoleDetailResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò"));
                
        RoleDetailResponse response = roleMapper.toDetailResponse(role);

        Set<String> rolePermissionCodes = role.getPermissions().stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        List<Permission> allPermissions = permissionRepository.findAll();
        List<RoleDetailResponse.PermissionInfo> permissionInfos = allPermissions.stream().map(p -> {
            RoleDetailResponse.PermissionInfo info = new RoleDetailResponse.PermissionInfo();
            info.setCode(p.getCode());
            info.setName(p.getName());
            info.setDescription(p.getDescription());
            info.setHasPermission(rolePermissionCodes.contains(p.getCode()));
            return info;
        }).collect(Collectors.toList());

        response.setPermissions(permissionInfos);
        
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò"));
        roleRepository.delete(role);
    }
    
    private void setPermissions(Role role, Set<String> permissionCodes) {
        Set<Permission> permissions = new HashSet<>();
        if (permissionCodes != null) {
            for (String code : permissionCodes) {
                permissionRepository.findByCode(code).ifPresent(permissions::add);
            }
        }
        role.setPermissions(permissions);
    }
}
