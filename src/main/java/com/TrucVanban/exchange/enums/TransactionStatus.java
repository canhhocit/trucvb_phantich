package com.TrucVanban.exchange.enums;

public enum TransactionStatus {
    RECEIVED("Mới nhận"),
    VALIDATED("Request đã vượt qua bước kiểm tra hợp lệ"),
    ROUTED("Nằm trong Queue chờ bốc"),
    DISPATCHED("Đang gọi HTTP sang đích"),
    DELIVERED("Agency đích đã tiếp nhận thành công"),
    FAILED("Đứt mạng, Sập endpoint");

    public final String description;

    TransactionStatus(String description) {
        this.description = description;
    }
}
