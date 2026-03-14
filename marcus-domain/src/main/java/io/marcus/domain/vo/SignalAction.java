package io.marcus.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SignalAction {
    OPEN_LONG,
    OPEN_SHORT,
    CLOSE_LONG,
    CLOSE_SHORT,
    UPDATE_TP_SL
}
