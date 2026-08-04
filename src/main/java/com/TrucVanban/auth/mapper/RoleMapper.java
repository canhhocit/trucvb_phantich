package com.TrucVanban.auth.mapper;

import com.TrucVanban.auth.dto.request.RoleRequest;
import com.TrucVanban.auth.dto.response.RoleDetailResponse;
import com.TrucVanban.auth.dto.response.RoleResponse;
import com.TrucVanban.auth.entity.Permission;
import com.TrucVanban.auth.entity.Role;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Role toEntity(RoleRequest request);

    @Mapping(target = "permissions", source = "permissions", qualifiedByName = "mapPermissions")
    RoleResponse toResponse(Role role);

    @Mapping(target = "permissions", ignore = true)
    RoleDetailResponse toDetailResponse(Role role);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "permissions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(RoleRequest request, @MappingTarget Role role);

    @Named("mapPermissions")
    default Set<String> mapPermissions(Set<Permission> permissions) {
        if (permissions == null) return null;
        return permissions.stream().map(Permission::getCode).collect(Collectors.toSet());
    }
}
