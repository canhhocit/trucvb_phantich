package com.TrucVanban.auth.controller;

import com.TrucVanban.auth.dto.request.CreateUserRequest;
import com.TrucVanban.auth.dto.request.UpdateUserRequest;
import com.TrucVanban.auth.dto.response.UserResponse;
import com.TrucVanban.auth.service.UserService;
import com.TrucVanban.shared.ResponseData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ResponseData<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse data = userService.createUser(request);

        ResponseData<UserResponse> response = ResponseData.<UserResponse>builder()
                .success(true)
                .message("Tạo người dùng thành công")
                .data(data)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseData<UserResponse>> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        UserResponse data = userService.updateUser(id, request);

        ResponseData<UserResponse> response = ResponseData.<UserResponse>builder()
                .success(true)
                .message("Cập nhật người dùng thành công")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseData<UserResponse>> getUserById(@PathVariable Long id) {
        UserResponse data = userService.getUserById(id);

        ResponseData<UserResponse> response = ResponseData.<UserResponse>builder()
                .success(true)
                .message("Lấy thông tin người dùng thành công")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<ResponseData<List<UserResponse>>> getAllUsers() {
        List<UserResponse> data = userService.getAllUsers();

        ResponseData<List<UserResponse>> response = ResponseData.<List<UserResponse>>builder()
                .success(true)
                .message("Lấy danh sách người dùng thành công")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseData<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);

        ResponseData<Void> response = ResponseData.<Void>builder()
                .success(true)
                .message("Xóa người dùng thành công")
                .build();

        return ResponseEntity.ok(response);
    }
}
