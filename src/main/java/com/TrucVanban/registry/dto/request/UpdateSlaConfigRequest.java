package com.TrucVanban.registry.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateSlaConfigRequest {
    @NotNull(message = "Thời gian tiếp nhận tối đa không được để trống")
    @Min(value = 0, message = "Thời gian tiếp nhận tối đa phải lớn hơn hoặc bằng 0")
    private Integer maxReceiveHours;
}
