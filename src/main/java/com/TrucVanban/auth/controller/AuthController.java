package com.TrucVanban.auth.controller;

import com.TrucVanban.auth.dto.request.LoginRequest;
import com.TrucVanban.auth.dto.request.RefreshTokenRequest;
import com.TrucVanban.auth.dto.response.TokenResponse;
import com.TrucVanban.auth.service.AuthService;
import com.TrucVanban.shared.ResponseData;
import com.TrucVanban.shared.exception.UnauthorizedException;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập")
    public ResponseEntity<ResponseData<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(ResponseData.<TokenResponse>builder()
                .success(true)
                .message("Đăng nhập thành công")
                .data(tokenResponse)
                .build());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Làm mới token")
    public ResponseEntity<ResponseData<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse tokenResponse = authService.refreshToken(request);
        return ResponseEntity.ok(ResponseData.<TokenResponse>builder()
                .success(true)
                .message("Làm mới token thành công")
                .data(tokenResponse)
                .build());
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất")
    public ResponseEntity<ResponseData<Void>> logout(
            @RequestHeader(value = "Authorization", required = true) String authorization,
            @Valid @RequestBody RefreshTokenRequest request) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing or invalid access token");
        }
        String accessToken = authorization.substring(7);

        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isEmpty()) {
            throw new UnauthorizedException("Missing refresh token");
        }

        authService.logout(accessToken, request.getRefreshToken());
        return ResponseEntity.ok(ResponseData.<Void>builder()
                .success(true)
                .message("Đăng xuất thành công")
                .build());
    }
}
