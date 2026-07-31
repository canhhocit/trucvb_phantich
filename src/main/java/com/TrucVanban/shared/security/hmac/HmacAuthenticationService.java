package com.TrucVanban.shared.security.hmac;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class HmacAuthenticationService {

    private final ApiKeyCacheService apiKeyCacheService;
    private final NonceStore nonceStore;
    private final SignatureCalculator signatureCalculator;
    private final HmacProperties hmacProperties;

    public void authenticate(HttpServletRequest request) {
        String apiKey = request.getHeader(hmacProperties.getHeader().getApiKey());
        String timestamp = request.getHeader(hmacProperties.getHeader().getTimestamp());
        String nonce = request.getHeader(hmacProperties.getHeader().getNonce());
        String signatureHeader = request.getHeader(hmacProperties.getHeader().getSignature());

        if (!StringUtils.hasText(apiKey) || !StringUtils.hasText(timestamp) || !StringUtils.hasText(nonce) || !StringUtils.hasText(signatureHeader)) {
            throw new HmacAuthenticationException.MissingAuthHeaderException("Missing required HMAC authentication headers");
        }

        long timestampSeconds;
        try {
            timestampSeconds = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new HmacAuthenticationException.TimestampSkewException("Invalid timestamp format");
        }

        long now = OffsetDateTime.now().toEpochSecond();
        long drift = Math.abs(now - timestampSeconds);
        if (drift > hmacProperties.getClockSkew().getSeconds()) {
            throw new HmacAuthenticationException.TimestampSkewException("Timestamp skew exceeds allowed window");
        }

        ApiKeyCacheValue apiKeyCacheValue = apiKeyCacheService.getApiKey(apiKey);
        if (apiKeyCacheValue == null) {
            throw new HmacAuthenticationException.ApiKeyInvalidException("API key is invalid or inactive");
        }

        if (apiKeyCacheValue.expiresAt() != null && OffsetDateTime.now().isAfter(apiKeyCacheValue.expiresAt())) {
            throw new HmacAuthenticationException.ApiKeyExpiredException("API key has expired");
        }

        if (!"ACTIVE".equals(apiKeyCacheValue.keyStatus()) || !"ACTIVE".equals(apiKeyCacheValue.agencyStatus())) {
            throw new HmacAuthenticationException.AgencyInactiveException("Agency or API key is not active");
        }

        byte[] body = readRequestBody(request);
        String canonicalString = signatureCalculator.calculateCanonicalString(
                request.getMethod(),
                request.getRequestURI(),
                request.getParameterMap(),
                apiKey,
                timestamp,
                nonce,
                body
        );

        String normalizedSignature = normalizeSignature(signatureHeader);
        String expectedSignature = signatureCalculator.calculateSignature(apiKeyCacheValue.secret(), canonicalString);

        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), normalizedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new HmacAuthenticationException.SignatureInvalidException("Signature mismatch");
        }

        if (!nonceStore.reserveNonce(apiKey, nonce, hmacProperties.getNonceTtl())) {
            throw new HmacAuthenticationException.ReplayDetectedException("Replay detected");
        }

        request.setAttribute("verified_org_id", apiKeyCacheValue.agencyId());
        request.setAttribute("verified_org_code", apiKeyCacheValue.agencyCode());
    }

    private String normalizeSignature(String signatureHeader) {
        if (signatureHeader == null) {
            return "";
        }
        String trimmed = signatureHeader.trim();
        if (trimmed.startsWith("v1=") || trimmed.startsWith("V1=")) {
            return trimmed.substring(trimmed.indexOf('=') + 1);
        }
        return trimmed;
    }

    private byte[] readRequestBody(HttpServletRequest request) {
        try {
            // Nếu đã wrap bởi CachedBodyRequestWrapper, lấy body từ cache (tránh đọc stream 2 lần)
            if (request instanceof CachedBodyRequestWrapper cached) {
                return cached.getCachedBody();
            }
            long contentLength = request.getContentLengthLong();
            if (contentLength > 0 && contentLength > hmacProperties.getMaxBodySize()) {
                throw new IllegalStateException("Request body exceeds configured max size");
            }
            return request.getInputStream().readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read request body for HMAC validation", e);
        }
    }
}
