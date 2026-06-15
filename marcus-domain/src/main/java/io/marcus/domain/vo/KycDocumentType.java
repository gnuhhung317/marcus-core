package io.marcus.domain.vo;

public enum KycDocumentType {
    ID_CARD_FRONT("Ảnh mặt trước CMND/CCCD"),
    ID_CARD_BACK("Ảnh mặt sau CMND/CCCD"),
    SELFIE("Ảnh chân dung selfie"),
    VIDEO("Video xác thực khuôn mặt");

    private final String description;

    KycDocumentType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
