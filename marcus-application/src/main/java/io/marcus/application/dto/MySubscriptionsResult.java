package io.marcus.application.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record MySubscriptionsResult(
        String wsToken,
        String localExecutorInstruction,
        List<SubscriptionSummaryResult> subscriptions
) {
}