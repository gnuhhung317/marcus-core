package io.marcus.domain.model;

import io.marcus.domain.vo.BotStatus;

import java.util.List;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)

public class Bot extends BaseModel{
    private String botId;
    private String developerId;
    private String description;
    private BotStatus status;
    private String tradingPair;
    private List<String> supportedExchanges;
}
