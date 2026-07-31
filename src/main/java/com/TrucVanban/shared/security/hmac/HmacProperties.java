package com.TrucVanban.shared.security.hmac;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "security.hmac")
@Data
public class HmacProperties {

    private boolean enabled = true;
    private Header header = new Header();
    private String algorithm = "HmacSHA256";
    private Duration clockSkew = Duration.ofSeconds(300);
    private Duration nonceTtl = Duration.ofSeconds(300);
    private Duration cacheTtl = Duration.ofMinutes(30);
    private Duration negativeCacheTtl = Duration.ofSeconds(60);
    private Duration reconcileInterval = Duration.ofHours(1);
    private long maxBodySize = 256 * 1024;
    private String masterKey;
    private List<String> protectedPaths = new ArrayList<>();

    @Data
    public static class Header {
        @NotBlank
        private String apiKey = "X-Api-Key";
        @NotBlank
        private String timestamp = "X-Timestamp";
        @NotBlank
        private String nonce = "X-Nonce";
        @NotBlank
        private String signature = "X-Signature";
    }
}
