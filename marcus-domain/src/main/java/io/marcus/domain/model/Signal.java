package io.marcus.domain.model;

import io.marcus.domain.vo.SignalAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import io.marcus.domain.vo.SignalStatus;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Signal extends BaseModel {
    private String signalId;
    private String botId;
    private SignalAction action;
    private BigDecimal entry;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit;
    private SignalStatus status;
    private LocalDateTime generatedTimestamp;
    private Map<String, Object> metadata; // <key, value>
}
