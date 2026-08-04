package com.TrucVanban.auth.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleDetailResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PermissionInfo> permissions;

    @Data
    public static class PermissionInfo {
        private String code;
        private String name;
        private String description;
        private boolean hasPermission;
    }
}
