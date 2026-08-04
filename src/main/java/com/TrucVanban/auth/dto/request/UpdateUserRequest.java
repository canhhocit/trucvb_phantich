package com.TrucVanban.auth.dto.request;

import com.TrucVanban.auth.enums.UserStatus;
import lombok.Data;
import java.util.Set;

@Data
public class UpdateUserRequest {
    private String fullName;
    private String email;
    private String phoneNumber;
    private UserStatus status;
    private Set<String> roles;
}
