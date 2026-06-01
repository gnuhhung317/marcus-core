package io.marcus.domain.vo;

import java.util.Locale;

public enum Role {
    ADMIN("Quản trị viên"),
    TRADER("Nhà giao dịch"),
    DEVELOPER("Nhà phát triển");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static Role fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Role value must not be blank");
        }

        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        if ("USER".equals(normalizedValue)) {
            return TRADER;
        }

        return Role.valueOf(normalizedValue);
    }
}
