package io.marcus.domain.vo;

public enum KycStatus {
    PENDING_UPLOAD("Chờ tải lên"),
    UPLOADED("Đã tải lên"),
    VERIFYING("Đang kiểm tra tính hợp lệ"),
    APPROVED_FOR_REVIEW("Hợp lệ - Chờ phê duyệt"),
    APPROVED("Đã phê duyệt"),
    REJECTED("Đã từ chối");

    private final String description;

    KycStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
