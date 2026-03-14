package io.marcus.domain.model;

import io.marcus.domain.vo.SignalAction;

import java.util.Map;
import java.util.Objects;

import io.marcus.domain.vo.SignalStatus;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Signal extends BaseModel {
    private String signalId;
    private String botId;
    private SignalAction action;
    private Float entry;
    private Float stopLoss;
    private Float takeProfit;
    private SignalStatus status;
    private Map<String, Object> metadata; // <key, value>
}
