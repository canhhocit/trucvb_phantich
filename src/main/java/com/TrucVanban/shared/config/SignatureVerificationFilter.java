package com.TrucVanban.shared.config;

import com.TrucVanban.exchange.service.AuditLogService;
import com.TrucVanban.registry.entity.Certificate;
import com.TrucVanban.registry.service.RegistryService;
import com.TrucVanban.shared.utils.CanonicalStringBuilder;
import com.TrucVanban.shared.utils.SignatureVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SignatureVerificationFilter - Interceptor bảo mật 3 lớp cho endpoint POST /api/exchange.
 *
 * [Chốt 1] Khống chế thời gian (Anti Replay Attack) - ±5 phút
 * [Chốt 2] Tra cứu danh tính (Certificate Lookup) - Caffeine Cache → DB
 * [Chốt 3] Đối sánh mật mã (Cryptographic Verification) - RSA SHA256withRSA
 */
@Slf4j
@RequiredArgsConstructor
public class SignatureVerificationFilter extends OncePerRequestFilter {

    private static final String EXCHANGE_PATH = "/exchange";
    private static final int MAX_TIME_DIFF_MINUTES = 5;
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private final RegistryService registryService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final CanonicalStringBuilder canonicalStringBuilder;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !(HttpMethod.POST.matches(request.getMethod())
                && PATH_MATCHER.match(EXCHANGE_PATH, request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        byte[] bodyBytes = request.getInputStream().readAllBytes();
        String bodyStr = new String(bodyBytes, StandardCharsets.UTF_8);

        log.info("[SignatureFilter] Nhận được body từ Client ({} bytes)", bodyBytes.length);

        JsonNode body;
        try {
            body = objectMapper.readTree(bodyStr);
        } catch (Exception e) {
            log.warn("[SignatureFilter] JSON body không hợp lệ: {}", e.getMessage());
            writeErrorResponse(response, HttpStatus.BAD_REQUEST,
                    "Dữ liệu đầu vào không hợp lệ. Vui lòng kiểm tra định dạng JSON.");
            return;
        }

        String senderCode = getTextSafe(body, "senderCode");
        String serialNumber = getTextSafe(body, "certificateSerialNumber");
        String timestampStr = getTextSafe(body, "timestamp");
        String signature = getTextSafe(body, "signature");
        String documentCode = getTextSafe(body, "documentCode");
        String payloadChecksum = getTextSafe(body, "payloadChecksum");
        List<String> receivers = getReceiversSafe(body);

        log.info("[SignatureFilter] Bắt đầu xác minh gói tin: sender={}, serial={}", senderCode, serialNumber);

        // CHỐT CHẶN 1: KHỐNG CHẾ THỜI GIAN (Anti Replay Attack)
        if (timestampStr == null || timestampStr.isBlank()) {
            log.warn("[SignatureFilter] [Chốt 1] Thiếu trường timestamp. sender={}", senderCode);
            auditLogService.log("SIGNATURE_REJECTED", "SYSTEM", senderCode, "FAILURE",
                    buildDetail("Thiếu trường timestamp", serialNumber, null), null, null);
            writeErrorResponse(response, HttpStatus.BAD_REQUEST,
                    "Trường timestamp là bắt buộc. Định dạng ISO 8601 (VD: 2026-06-27T14:00:00+07:00)");
            return;
        }

        OffsetDateTime packetTime;
        try {
            packetTime = OffsetDateTime.parse(timestampStr);
        } catch (Exception e) {
            log.warn("[SignatureFilter] [Chốt 1] Timestamp không đúng định dạng ISO 8601: {}", timestampStr);
            auditLogService.log("REPLAY_ATTACK_TIMESTAMP_INVALID", "SYSTEM", senderCode, "FAILURE",
                    buildDetail("Timestamp không đúng định dạng ISO 8601: " + timestampStr, serialNumber, null), null, null);
            writeErrorResponse(response, HttpStatus.BAD_REQUEST,
                    "Timestamp không đúng định dạng ISO 8601.");
            return;
        }

        OffsetDateTime serverTime = OffsetDateTime.now();
        long diffMinutes = Math.abs(Duration.between(packetTime, serverTime).toMinutes());

        if (diffMinutes > MAX_TIME_DIFF_MINUTES) {
            log.warn("[SignatureFilter] [Chốt 1] REPLAY ATTACK DETECTED! sender={}, drift={}m", senderCode, diffMinutes);
            auditLogService.log("REPLAY_ATTACK_DETECTED", "ORGANIZATION", senderCode, "FAILURE",
                    buildDetail(String.format("Lệch thời gian %d phút (max %d phút). T_packet=%s, T_server=%s",
                            diffMinutes, MAX_TIME_DIFF_MINUTES, packetTime, serverTime), serialNumber, null), null, null);
            writeErrorResponse(response, HttpStatus.REQUEST_TIMEOUT,
                    String.format("Từ chối gói tin: Replay Attack phát hiện. Lệch thời gian %d phút (giới hạn %d phút).",
                            diffMinutes, MAX_TIME_DIFF_MINUTES));
            return;
        }

        log.info("[SignatureFilter] [Chốt 1] OK - Lệch thời gian {}m < {}m", diffMinutes, MAX_TIME_DIFF_MINUTES);

        // CHỐT CHẶN 2: TRA CỨU DANH TÍNH (Certificate Lookup)
        if (serialNumber == null || serialNumber.isBlank()) {
            log.warn("[SignatureFilter] [Chốt 2] Thiếu trường certificateSerialNumber. sender={}", senderCode);
            writeErrorResponse(response, HttpStatus.BAD_REQUEST,
                    "Trường certificateSerialNumber là bắt buộc.");
            return;
        }

        Certificate certificate = registryService.findActiveCertificateBySerialNumber(serialNumber);

        if (certificate == null) {
            log.warn("[SignatureFilter] [Chốt 2] Không tìm thấy chứng thư ACTIVE: serialNumber={}", serialNumber);
            auditLogService.log("CERT_NOT_FOUND", "ORGANIZATION", senderCode, "FAILURE",
                    buildDetail("Không tìm thấy chứng thư ACTIVE hoặc chứng thư đã hết hạn. serialNumber=" + serialNumber,
                            serialNumber, null), null, null);
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED,
                    "Chứng thư số không hợp lệ hoặc đã hết hạn. Vui lòng cập nhật chứng thư mới.");
            return;
        }

        log.info("[SignatureFilter] [Chốt 2] OK - Tìm thấy chứng thư ACTIVE: serial={}, org_id={}",
                serialNumber, certificate.getOrganizationId());

        // CHỐT CHẶN 3: ĐỐI SÁNH mm

        if (signature == null || signature.isBlank()) {
            log.warn("[SignatureFilter] [Chốt 3] Thiếu trường signature. sender={}", senderCode);
            writeErrorResponse(response, HttpStatus.BAD_REQUEST,
                    "Trường signature là bắt buộc.");
            return;
        }

        String canonicalString = canonicalStringBuilder.build(
                serialNumber, documentCode, payloadChecksum, receivers, senderCode, timestampStr
        );

        log.info("[SignatureFilter] [Chốt 3] Canonical String (Gateway):\n{}", canonicalString);

        boolean isValid = SignatureVerifier.verify(canonicalString, signature, certificate.getPublicKey());

        if (!isValid) {
            log.warn("[SignatureFilter] [Chốt 3] CHỮ KÝ KHÔNG HỢP LỆ! sender={}, serial={}", senderCode, serialNumber);
            auditLogService.log("SIGNATURE_INVALID", "ORGANIZATION", senderCode, "FAILURE",
                    buildDetail("Chữ ký không hợp lệ - Gói tin có thể bị sửa đổi sau khi ký.",
                            serialNumber, canonicalString), null, null);
            writeErrorResponse(response, HttpStatus.UNAUTHORIZED,
                    "Xác minh chữ ký số thất bại. Gói tin không toàn vẹn hoặc chữ ký sai.");
            return;
        }

        log.info("[SignatureFilter] [Chốt 3] OK - CHỮ KÝ HỢP LỆ. sender={}", senderCode);

        // Ghi audit log thành công
        auditLogService.log("SIGNATURE_VERIFIED", "ORGANIZATION", senderCode, "SUCCESS",
                buildDetail("Xác minh chữ ký thành công. 3/3 chốt chặn đã vượt qua.",
                        serialNumber, canonicalString), null, null);

        CachedBodyRequestWrapper cachedRequest = new CachedBodyRequestWrapper(request, bodyBytes);
        cachedRequest.setAttribute("verified_org_id", certificate.getOrganizationId());
        cachedRequest.setAttribute("verified_serial_number", serialNumber);

        // Cho phép request đi tiếp vào Controller
        filterChain.doFilter(cachedRequest, response);
    }


    private String getTextSafe(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return (field != null && !field.isNull()) ? field.asText() : null;
    }

    private List<String> getReceiversSafe(JsonNode node) {
        JsonNode receiversNode = node.get("receiverCodes");
        List<String> result = new ArrayList<>();
        if (receiversNode != null && receiversNode.isArray()) {
            receiversNode.forEach(r -> result.add(r.asText()));
        }
        return result;
    }

    private String buildDetail(String message, String serialNumber, String canonicalString) {
        try {
            var map = new java.util.LinkedHashMap<String, String>();
            map.put("message", message);
            if (serialNumber != null) map.put("certificateSerialNumber", serialNumber);
            if (canonicalString != null) map.put("canonicalString", canonicalString);
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{\"message\":\"" + message + "\"}";
        }
    }

    private void writeErrorResponse(HttpServletResponse response,
                                    HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String jsonBody = String.format("{\"success\":false,\"message\":\"%s\",\"data\":null}",
                message.replace("\"", "\\\""));
        response.getWriter().write(jsonBody);
    }

    /**
     * Custom HttpServletRequestWrapper cho phép đọc lại body nhiều lần.
     */
    private static class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] cachedBody;

        public CachedBodyRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.cachedBody = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            return new CachedServletInputStream(cachedBody);
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(
                    new InputStreamReader(new ByteArrayInputStream(cachedBody), StandardCharsets.UTF_8));
        }

        @Override
        public int getContentLength() {
            return cachedBody.length;
        }

        @Override
        public long getContentLengthLong() {
            return cachedBody.length;
        }
    }

    /**
     * Custom ServletInputStream đọc từ byte array thay vì socket.
     */
    private static class CachedServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream inputStream;

        public CachedServletInputStream(byte[] body) {
            this.inputStream = new ByteArrayInputStream(body);
        }

        @Override
        public boolean isFinished() {
            return inputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // Không cần listener cho cached body
        }

        @Override
        public int read() {
            return inputStream.read();
        }

        @Override
        public int read(byte[] b, int off, int len) {
            return inputStream.read(b, off, len);
        }

        @Override
        public int available() {
            return inputStream.available();
        }
    }
}
