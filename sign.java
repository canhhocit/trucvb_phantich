import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Script ký RSA SHA256withRSA để test POST /exchange
 * Chạy: java sign.java
 */
public class sign {
    public static void main(String[] args) throws Exception {
//feat(auth): implement API Key + HMAC-SHA256 authentication for inter-system APIs
        // ═══════════════════════════════════════
        // THAY 3 GIÁ TRỊ NÀY TRƯỚC KHI CHẠY
        String documentCode    = "VB-2020";
        String payloadChecksum = "c510fe5e816ae4f1502c58c597711469c1e8d7010790c8fbd6c84654f528849d";
        String storagePath     = "trucvanban/part7_1.pdf";
        // ═══════════════════════════════════════

        String serialNumber   = "SN-2027-ABC999";
        String senderCode     = "000.00.00.H41";
        String receivers      = "000.00.00.H42";
        String privateKeyPath = "d:\\code\\TeamthayCuong\\TRUCVB-project\\HeThongTrucVanBan\\private_key.pem";

        // Timestamp hiện tại UTC
        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .format(ZonedDateTime.now(ZoneOffset.UTC));

        // Tạo canonical string (sort A-Z theo key, URL-encode value)
        String canonical =
            "certificate_serial_number:" + encode(serialNumber)  + "\n" +
            "document_code:"             + encode(documentCode)   + "\n" +
            "payload_checksum:"          + encode(payloadChecksum)+ "\n" +
            "receivers:"                 + encode(receivers)      + "\n" +
            "sender_code:"               + encode(senderCode)     + "\n" +
            "timestamp:"                 + encode(timestamp);

        System.out.println("=== CANONICAL STRING ===");
        System.out.println(canonical);

        // Đọc private key PKCS8
        String pem = Files.readString(Path.of(privateKeyPath))
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(pem);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);

        // Ký SHA256withRSA
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(canonical.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getEncoder().encodeToString(sig.sign());

        // Gọi thẳng API /exchange — không cần Postman
        String json = "{" +
            "\"senderCode\":\"" + senderCode + "\"," +
            "\"receiverCodes\":[\"" + receivers + "\"]," +
            "\"documentCode\":\"" + documentCode + "\"," +
            "\"payloadChecksum\":\"" + payloadChecksum + "\"," +
            "\"certificateSerialNumber\":\"" + serialNumber + "\"," +
            "\"timestamp\":\"" + timestamp + "\"," +
            "\"signature\":\"" + signature + "\"," +
            "\"storagePath\":\"" + storagePath + "\"," +
            "\"title\":\"Van ban thu nghiem\"," +
            "\"documentType\":\"OFFICIAL\"," +
            "\"priority\":1," +
            "\"summary\":\"Tom tat noi dung van ban\"" +
            "}";

        // Gọi API
        java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create("http://localhost:8080/api/v1/exchange"))
            .header("Content-Type", "application/json; charset=UTF-8")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
            .build();

        java.net.http.HttpResponse<String> httpResponse = client.send(
            httpRequest, java.net.http.HttpResponse.BodyHandlers.ofString());

        System.out.println("\n=== RESPONSE ===");
        System.out.println("Status: " + httpResponse.statusCode());
        System.out.println("Body  : " + httpResponse.body());
    }

    static String encode(String value) throws Exception {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
            .replace("+", "%20")
            .replace("%7E", "~")
            .replace("*", "%2A");
    }
}
