package com.TrucVanban.shared.security.hmac;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SignatureCalculator {

    private final HmacProperties hmacProperties;

    public String calculateCanonicalString(String method,
                                           String path,
                                           Map<String, String[]> queryParameters,
                                           String apiKey,
                                           String timestamp,
                                           String nonce,
                                           byte[] body) {
        String normalizedMethod = method == null ? "" : method.toUpperCase();
        String normalizedPath = normalizePath(path);
        String canonicalQuery = buildCanonicalQuery(queryParameters);
        String bodyHash = computeSha256Hex(body == null ? new byte[0] : body);

        return String.join("\n",
                normalizedMethod,
                normalizedPath,
                canonicalQuery,
                apiKey == null ? "" : apiKey,
                timestamp == null ? "" : timestamp,
                nonce == null ? "" : nonce,
                bodyHash
        );
    }

    public String calculateSignature(String secret, String canonicalString) {
        try {
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, hmacProperties.getAlgorithm());
            Mac mac = Mac.getInstance(hmacProperties.getAlgorithm());
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(canonicalString.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(rawHmac);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate HMAC signature", e);
        }
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return URI.create(path).getPath();
    }

    private String buildCanonicalQuery(Map<String, String[]> queryParameters) {
        if (queryParameters == null || queryParameters.isEmpty()) {
            return "";
        }
        Map<String, String[]> sorted = new TreeMap<>(queryParameters);
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String[]> entry : sorted.entrySet()) {
            String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            for (String value : entry.getValue()) {
                parts.add(key + "=" + URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8));
            }
        }
        return String.join("&", parts);
    }

    private String computeSha256Hex(byte[] payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload);
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute SHA-256 hash", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
