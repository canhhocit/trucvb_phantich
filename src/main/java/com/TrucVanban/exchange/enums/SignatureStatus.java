package com.TrucVanban.exchange.enums;

public enum SignatureStatus {
    PENDING("Chờ đánh giá"),
    VALID("Chữ ký hợp lệ"),
    INVALID("Sai chữ ký"),
    CERT_EXPIRED("Chứng thư hỏng");

    public final String description;

    SignatureStatus(String description) {
        this.description = description;
    }
}
