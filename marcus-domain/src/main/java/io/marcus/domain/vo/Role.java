package io.marcus.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {
    ADMIN("Quản trị viên"),
    TRADER("Nhà giao dịch"),
    DEVELOPER("Nhà phát triển");

    private final String description;
}
