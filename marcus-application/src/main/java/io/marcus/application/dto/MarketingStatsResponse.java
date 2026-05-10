package io.marcus.application.dto;

import lombok.Builder;

@Builder
public record MarketingStatsResponse(
        long verifiedDevelopers,
        long activeCloudExecutors,
        String systemUptime,
        int supportedExchanges
) {
}
