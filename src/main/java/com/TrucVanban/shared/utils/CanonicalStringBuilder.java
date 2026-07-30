package com.TrucVanban.shared.utils;

import com.TrucVanban.exchange.dto.request.send.ExchangeDocumentRequest; // Import DTO của bạn
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

/**
 * Chuẩn hóa chuỗi ký (Canonical String Builder) theo quy tắc nghiêm ngặt:
 * 1. Tất cả Key viết thường (lowercase)
 * 2. Sắp xếp Key theo bảng chữ cái A-Z (TreeMap)
 * 3. Mảng receivers sắp xếp tăng dần trước khi nối chuỗi
 * 4. Mã hóa Value theo URL-Encode chuẩn RFC 3986
 * 5. Nối mỗi cặp key:value bằng ký tự \n
 */
@Component // Đánh dấu Component để có thể Inject vào Service nếu cần
public class CanonicalStringBuilder {

    /**
     * Hàm tiện ích nhận thẳng Request Object để code Service được sạch sẽ
     */
    public String build(ExchangeDocumentRequest request) {
        return build(
                request.getCertificateSerialNumber(),
                request.getDocumentCode(),
                request.getPayloadChecksum(),
                request.getReceiverCodes(),
                request.getSenderCode(),
                request.getTimestamp()
        );
    }

    /**
     * Xây dựng Canonical String từ các trường dữ liệu lõi của gói tin.
     */
    public String build(
            String certificateSerialNumber,
            String documentCode,
            String payloadChecksum,
            List<String> receivers,
            String senderCode,
            String timestamp
    ) {
        // Bước 1: Check Null an toàn và sắp xếp mảng receivers
        List<String> sortedReceivers = new ArrayList<>();
        if (receivers != null && !receivers.isEmpty()) {
            sortedReceivers.addAll(receivers);
            Collections.sort(sortedReceivers);
        }
        String receiversValue = String.join(",", sortedReceivers);

        // Bước 2: Đưa vào TreeMap để tự động sort key A-Z (lowercase)
        TreeMap<String, String> canonicalMap = new TreeMap<>();
        canonicalMap.put("certificate_serial_number", certificateSerialNumber);
        canonicalMap.put("document_code", documentCode);
        canonicalMap.put("payload_checksum", payloadChecksum);
        canonicalMap.put("receivers", receiversValue);
        canonicalMap.put("sender_code", senderCode);
        canonicalMap.put("timestamp", timestamp);

        // Bước 3: URL-Encode các giá trị (RFC 3986) và nối bằng \n
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var entry : canonicalMap.entrySet()) {
            if (!first) {
                sb.append('\n');
            }
            sb.append(entry.getKey())
                    .append(':')
                    .append(urlEncode(entry.getValue()));
            first = false;
        }

        return sb.toString();
    }

    /**
     * URL-Encode tuyệt đối chuẩn RFC 3986
     */
    private static String urlEncode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")   // Space thành %20
                .replace("%7E", "~")   // Dấu ngã không mã hóa
                .replace("*", "%2A");  // BẮT BUỘC: Dấu sao phải mã hóa theo chuẩn RFC
    }
}