package com.TrucVanban.registry.mapper;

import com.TrucVanban.registry.dto.request.UpdateSlaConfigRequest;
import com.TrucVanban.registry.dto.response.UpdateSlaConfigResponse;
import com.TrucVanban.registry.entity.SlaConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SlaConfigMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "documentPriority", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SlaConfiguration toEntity(UpdateSlaConfigRequest request);

    UpdateSlaConfigResponse toResponse(SlaConfiguration entity);
}
