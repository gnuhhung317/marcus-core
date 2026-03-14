package io.marcus.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExecutorTokenStatus {
    VALID,
    EXPIRED,
    REVOKED
}
