package com.TrucVanban.auth.service.impl;

import com.TrucVanban.auth.dto.request.LoginRequest;
import com.TrucVanban.auth.dto.request.RefreshTokenRequest;
import com.TrucVanban.auth.dto.response.TokenResponse;
import com.TrucVanban.auth.entity.Permission;
import com.TrucVanban.auth.entity.RefreshToken;
import com.TrucVanban.auth.entity.Role;
import com.TrucVanban.auth.entity.User;
import com.TrucVanban.auth.enums.UserStatus;
import com.TrucVanban.auth.repository.RefreshTokenRepository;
import com.TrucVanban.auth.repository.UserRepository;
import com.TrucVanban.auth.service.AuthService;
import com.TrucVanban.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.access-token-expiration:3600}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:604800}")
    private long refreshTokenExpiration;

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Tên đăng nhập hoặc mật khẩu không hợp lệ"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản đã bị khóa");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("Tên đăng nhập hoặc mật khẩu không hợp lệ");
        }

        return generateTokens(user);
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token không hợp lệ"));

        if (storedToken.isRevoked()) {
            throw new UnauthorizedException("Refresh token đã bị thu hồi");
        }

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token đã hết hạn");
        }

        try {
            jwtDecoder.decode(request.getRefreshToken());
            User user = storedToken.getUser();

            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new UnauthorizedException("Tài khoản đã bị khóa");
            }

            // Revoke the old token (Token Rotation) to prevent reuse
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);

            return generateTokens(user);
        } catch (Exception e) {
            throw new UnauthorizedException("Refresh token không hợp lệ hoặc đã hết hạn");
        }
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            try {
                Jwt jwt = jwtDecoder.decode(accessToken);
                Instant expiresAt = jwt.getExpiresAt();
                if (expiresAt != null) {
                    long ttl = java.time.Duration.between(Instant.now(), expiresAt).getSeconds();
                    if (ttl > 0) {
                        redisTemplate.opsForValue().set(
                                "blacklist:" + accessToken, 
                                "true", 
                                ttl, 
                                java.util.concurrent.TimeUnit.SECONDS
                        );
                    }
                }
            } catch (JwtException e) {
                // Ignore invalid or expired token
            }
        }
        
        if (refreshToken != null) {
            refreshTokenRepository.findByToken(refreshToken).ifPresent(storedToken -> {
                storedToken.setRevoked(true);
                refreshTokenRepository.save(storedToken);
            });
        }
    }

    private TokenResponse generateTokens(User user) {
        Instant now = Instant.now();

        List<String> roles = user.getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toList());

        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getCode)
                .distinct()
                .collect(Collectors.toList());

        JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                .issuer("HeThongTrucVanBan")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTokenExpiration))
                .subject(user.getUsername())
                .claim("type", "ACCESS")
                .claim("roles", roles)
                .claim("permissions", permissions)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, accessClaims)).getTokenValue();

        JwtClaimsSet refreshClaims = JwtClaimsSet.builder()
                .issuer("HeThongTrucVanBan")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(refreshTokenExpiration))
                .subject(user.getUsername())
                .claim("type", "REFRESH")
                .build();

        String refreshToken = jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, refreshClaims)).getTokenValue();

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .user(user)
                .expiresAt(now.plusSeconds(refreshTokenExpiration))
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
