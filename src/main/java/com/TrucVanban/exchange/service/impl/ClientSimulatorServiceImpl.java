package com.TrucVanban.exchange.service.impl;

import com.TrucVanban.exchange.dto.request.send.ExchangeDocumentRequest;
import com.TrucVanban.exchange.dto.request.send.SignAndBuildRequest;
import com.TrucVanban.exchange.dto.response.FileUploadResponse;
import com.TrucVanban.exchange.service.ClientSimulatorService;
import com.TrucVanban.shared.utils.CanonicalStringBuilder;
import com.TrucVanban.shared.service.MinioService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClientSimulatorServiceImpl implements ClientSimulatorService {

    RestClient restClient;
    CanonicalStringBuilder canonicalStringBuilder;
    MinioService minioService;

    private static final String PRIVATE_KEY_PATH = "private_key.pem";

    @Override
    public List<FileUploadResponse> uploadFiles(MultipartFile[] files) throws Exception {
        if (files == null || files.length == 0) return new ArrayList<>();

        List<FileUploadResponse> results = new ArrayList<>(files.length);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        for (MultipartFile file : files) {
            String objectKey = minioService.upload(file);
            byte[] hashBytes = digest.digest(file.getBytes());

            // Thuật toán Hash sử dụng StringBuilder đơn giản dễ đọc
            String computedChecksum = computeHexChecksum(hashBytes);

            results.add(FileUploadResponse.builder()
                    .fileName(file.getOriginalFilename())
                    .storagePath(objectKey)
                    .payloadChecksum(computedChecksum)
                    .build());
        }
        return results;
    }

    @Override
    public ExchangeDocumentRequest signAndBuildPayload(SignAndBuildRequest request) throws Exception {
        ExchangeDocumentRequest internalRequest = new ExchangeDocumentRequest();
        internalRequest.setSenderCode(request.getSenderCode());
        internalRequest.setReceiverCodes(request.getReceiverCodes());
        internalRequest.setDocumentCode(request.getDocumentCode());
        internalRequest.setPayloadChecksum(request.getPayloadChecksum());
        internalRequest.setStoragePath(request.getStoragePath());
        internalRequest.setCertificateSerialNumber(request.getCertificateSerialNumber());
        internalRequest.setPriority(request.getPriority() != null ? request.getPriority() : 1);

        String currentTimestamp = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        internalRequest.setTimestamp(currentTimestamp);

        String canonicalString = canonicalStringBuilder.build(internalRequest);
        PrivateKey privateKey = loadPrivateKeyFromPem(PRIVATE_KEY_PATH);

        Signature signatureInstance = Signature.getInstance("SHA256withRSA");
        signatureInstance.initSign(privateKey);
        signatureInstance.update(canonicalString.getBytes(StandardCharsets.UTF_8));
        byte[] digitalSignatureBytes = signatureInstance.sign();

        internalRequest.setSignature(Base64.getEncoder().encodeToString(digitalSignatureBytes));
        return internalRequest;
    }

    @Override
    public Object processAndSend(MultipartFile file, String senderCode, List<String> receiverCodes,
                                 String documentCode, String certificateSerialNumber, Integer priority) throws Exception {

        // 1. Upload & Hash
        String objectKey = minioService.upload(file);
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(file.getBytes());
        String computedChecksum = computeHexChecksum(hashBytes);

        // 2. Build Request
        SignAndBuildRequest signReq = new SignAndBuildRequest();
        signReq.setSenderCode(senderCode);
        signReq.setReceiverCodes(receiverCodes);
        signReq.setDocumentCode(documentCode);
        signReq.setStoragePath(objectKey);
        signReq.setPayloadChecksum(computedChecksum);
        signReq.setCertificateSerialNumber(certificateSerialNumber);
        signReq.setPriority(priority);

        // 3. Gọi hàm tái sử dụng để Ký số
        ExchangeDocumentRequest finalPayload = signAndBuildPayload(signReq);

        // 4. Bắn sang Gateway
        String gatewayUrl = "http://localhost:8080/api/v1/exchange";
        log.info("[Simulator Service] RestClient gửi tới Gateway: {}", gatewayUrl);

        return restClient.post()
                .uri(gatewayUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(finalPayload)
                .retrieve()
                .body(Object.class);
    }

    // Tách riêng hàm thuật toán Hash chuẩn StringBuilder
    private String computeHexChecksum(byte[] hashBytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Đọc Private Key từ Server
    private PrivateKey loadPrivateKeyFromPem(String filePath) throws Exception {
        String keyContent = Files.readString(Paths.get(filePath));
        keyContent = keyContent.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");

        byte[] keyBytes = Base64.getDecoder().decode(keyContent);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }
}