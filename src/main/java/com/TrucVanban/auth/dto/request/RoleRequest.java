package com.TrucVanban.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Set;

@Data
public class RoleRequest {
    @NotBlank(message = "Code cannot be blank")
    private String code;
    
    @NotBlank(message = "Name cannot be blank")
    private String name;
    
    private String description;
    
    private Set<String> permissions;
}
