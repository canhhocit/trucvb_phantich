package com.TrucVanban.exchange.service.impl;

import com.TrucVanban.exchange.dto.request.send.ExchangeDocumentRequest;
import com.TrucVanban.exchange.service.ClientSimulatorService;
import com.TrucVanban.shared.utils.CanonicalStringBuilder;
import com.TrucVanban.shared.service.MinioService;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient; // Đổi sang RestClient
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClientSimulatorServiceImpl implements ClientSimulatorService {

    RestClient restClient; // Tiêm Bean RestClient từ file cấu hình của bạn vào đây
    CanonicalStringBuilder canonicalStringBuilder;
    MinioService minioService;
    String gatewayUrl;

    public ClientSimulatorServiceImpl(
            RestClient restClient,
            CanonicalStringBuilder canonicalStringBuilder,
            MinioService minioService,
            @Value("${app.client-simulator.gateway-url}") String gatewayUrl
    ) {
        this.restClient = restClient;
        this.canonicalStringBuilder = canonicalStringBuilder;
        this.minioService = minioService;
        this.gatewayUrl = gatewayUrl;
    }

    @Override
    public Object processAndSend(MultipartFile file, String senderCode, List<String> receiverCodes,
                                 String documentCode, String certificateSerialNumber, Integer priority) throws Exception {

        // 1. UPLOAD FILE LÊN MINIO VÀ LẤY STORAGE PATH
        String objectKey = minioService.upload(file);
        log.info("[Simulator Service] Đã upload thành công lên MinIO với tên: {}", objectKey);

        // 2. TÍNH TOÁN PAYLOAD CHECKSUM (SHA-256)
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(file.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        String computedChecksum = hexString.toString();

        // 3. KHỞI TẠO REQUEST LÕI
        ExchangeDocumentRequest internalRequest = new ExchangeDocumentRequest();
        internalRequest.setSenderCode(senderCode);
        internalRequest.setReceiverCodes(receiverCodes);
        internalRequest.setDocumentCode(documentCode);
        internalRequest.setPayloadChecksum(computedChecksum);
        internalRequest.setStoragePath(objectKey);
        internalRequest.setCertificateSerialNumber(certificateSerialNumber);
        internalRequest.setPriority(priority != null ? priority : 1);

        // 4. THIẾT LẬP THỜI GIAN THỰC CHUẨN ISO-8601
        String currentTimestamp = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        internalRequest.setTimestamp(currentTimestamp);

        // 5. ĐỌC KHÓA BÍ MẬT TỪ FILE .PEM VÀ THỰC HIỆN KÝ SỐ RSA
        String canonicalString = canonicalStringBuilder.build(internalRequest);
        PrivateKey privateKey = loadPrivateKeyFromPem("private_key.pem");

        Signature signatureInstance = Signature.getInstance("SHA256withRSA");
        signatureInstance.initSign(privateKey);
        signatureInstance.update(canonicalString.getBytes(StandardCharsets.UTF_8));
        byte[] digitalSignatureBytes = signatureInstance.sign();

        String base64Signature = Base64.getEncoder().encodeToString(digitalSignatureBytes);
        internalRequest.setSignature(base64Signature);
        log.info("[Simulator Service] Đã dập chữ ký số RSA thực tế thành công.");

        // 6. BẮN REQUEST JSON SANG TRỤC GATEWAY DÙNG RESTCLIENT FLUENT API
        log.info("[Simulator Service] Đang dùng RestClient chuyển tiếp gói tin JSON tới Gateway: {}", gatewayUrl);

        // Code viết theo phong cách Fluent rất sạch sẽ, tự động map Object thành JSON và parse Body kết quả
        return restClient.post()
                .uri(gatewayUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON) // Khai báo rõ đầu nhận cũng mong muốn JSON
                .body(internalRequest) // RestClient sẽ tự dùng MappingJackson2HttpMessageConverter để chuyển đổi List<String> thành mảng JSON [] chuẩn chỉ
                .retrieve()
                .body(Object.class);
    }

    private PrivateKey loadPrivateKeyFromPem(String filePath) throws Exception {
        String keyContent = Files.readString(Paths.get(filePath));
        keyContent = keyContent.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }
}
