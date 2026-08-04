package com.TrucVanban.auth.service;

import com.TrucVanban.auth.dto.request.CreateUserRequest;
import com.TrucVanban.auth.dto.request.UpdateUserRequest;
import com.TrucVanban.auth.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse createUser(CreateUserRequest request);
    UserResponse updateUser(Long id, UpdateUserRequest request);
    UserResponse getUserById(Long id);
    List<UserResponse> getAllUsers();
    void deleteUser(Long id);
}
