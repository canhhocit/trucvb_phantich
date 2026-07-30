package com.TrucVanban.registry.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSlaConfigResponse {
    private Integer documentPriority;
    private Integer maxReceiveHours;
}
