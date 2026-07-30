package com.TrucVanban.shared.utils;

import lombok.extern.slf4j.Slf4j;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * RSA SHA256withRSA Signature Verifier.
 * Xác minh chữ ký số giả lập theo thuật toán SHA256withRSA.
 */
@Slf4j
public class SignatureVerifier {

    private SignatureVerifier() {
        // Utility class
    }

    /**
     * Xác minh chữ ký số.
     */
    public static boolean verify(String canonicalString, String signatureBase64, String publicKeyPem) {
        try {
            // Lấy public key từ PEM
            PublicKey publicKey = loadPublicKey(publicKeyPem);

            // Decode signature từ Base64
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);

            // Xác minh chữ ký bằng SHA256withRSA
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(canonicalString.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            return sig.verify(signatureBytes);
        } catch (Exception e) {
            log.warn("[SignatureVerifier] Xác minh chữ ký thất bại: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Load Public Key từ chuỗi PEM (có hoặc không có header/footer)
     */
    private static PublicKey loadPublicKey(String pem) throws Exception {
        // Loại bỏ header/footer PEM nếu có
        String cleanPem = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN RSA PUBLIC KEY-----", "")
                .replace("-----END RSA PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(cleanPem);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }
}
