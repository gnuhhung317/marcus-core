package io.marcus.domain.model;

import io.marcus.domain.vo.BotStatus;

import java.util.List;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)

public class Bot extends BaseModel{
    private String botId;
    private String name;
    private String developerId;
    private String description;
    private BotStatus status;
    private String secretKey;
    private String apiKey;
    private String tradingPair;
    private String exchangeId;
}
