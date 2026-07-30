package com.TrucVanban.exchange.enums;

public enum DocumentStatus {
    ACTIVE("Bình thường"),
    RECALLED("Đã chết do bị thu hồi"),
    REPLACED("Đã chết do bị văn bản khác thay thế đè lên");

    public final String description;

    DocumentStatus(String description) {
        this.description = description;
    }
}
