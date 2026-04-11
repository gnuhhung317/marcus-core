package io.marcus.application.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record SubscriptionSummaryResult(
        String botId,
        String botName,
        String tradingPair,
        String status,
        LocalDateTime subscribedAt
        ) {

}
