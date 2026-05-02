package io.marcus.application.dto;

import lombok.Builder;

@Builder
public record SubscribeBotResult(
        String botId,
        String wsToken,
        String status
        ) {

}
