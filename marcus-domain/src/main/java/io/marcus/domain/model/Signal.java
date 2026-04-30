package io.marcus.domain.model;

import io.marcus.domain.vo.SignalAction;
import io.marcus.domain.vo.SignalStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Signal extends BaseModel {
    private String signalId;
    private String botId;
    private String symbol;
    private SignalAction action;
    private BigDecimal entry;
    private BigDecimal stopLoss;
    private BigDecimal takeProfit;
    private SignalStatus status;
    private LocalDateTime generatedTimestamp;
    private Map<String, Object> metadata;
}
