package com.TrucVanban.exchange.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.Converter;

public enum BusinessStatusCode {

    ARRIVED_DESTINATION("01", "Đã đến máy chủ đích"),
    REJECTED_INITIAL("02", "Từ chối tiếp nhận ban đầu / Từ chối lệnh Thu hồi"),
    ACCEPTED("03", "Đã tiếp nhận/Vào sổ công văn / Đồng ý lệnh Thu hồi"),
    ASSIGNED("04", "Lãnh đạo đã phân công xử lý"),
    IN_PROGRESS("05", "Chuyên viên đang giải quyết"),
    COMPLETED("06", "Hoàn thành toàn bộ công việc"),
    RECALL_REQUESTED("13", "Yêu cầu lấy lại"),
    RECALL_ACCEPTED("15", "Đồng ý lấy lại / Đồng ý lệnh Cập nhật nội dung"),
    RECALL_REJECTED("16", "Từ chối lấy lại / Từ chối lệnh Cập nhật nội dung");

    private final String code;
    private final String description;

    BusinessStatusCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static BusinessStatusCode fromCode(String code) {
        for (BusinessStatusCode status : values()) {
            if (status.code.equals(code)) return status;
        }
        throw new IllegalArgumentException("Mã trạng thái không hợp lệ: " + code);
    }

    @jakarta.persistence.Converter(autoApply = true)
    public static class Converter implements jakarta.persistence.AttributeConverter<BusinessStatusCode, String> {
        @Override
        public String convertToDatabaseColumn(BusinessStatusCode attribute) {
            return attribute == null ? null : attribute.getCode();
        }

        @Override
        public BusinessStatusCode convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fromCode(dbData);
        }
    }
}
