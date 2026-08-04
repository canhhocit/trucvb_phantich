package com.TrucVanban.auth.service;

import com.TrucVanban.auth.dto.request.LoginRequest;
import com.TrucVanban.auth.dto.request.RefreshTokenRequest;
import com.TrucVanban.auth.dto.response.TokenResponse;

public interface AuthService {
    TokenResponse login(LoginRequest request);
    TokenResponse refreshToken(RefreshTokenRequest request);
    void logout(String accessToken, String refreshToken);
}
