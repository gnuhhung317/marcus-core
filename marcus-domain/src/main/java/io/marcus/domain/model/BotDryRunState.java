package io.marcus.domain.model;

import java.util.List;

public record BotDryRunState(
        BotDryRunPortfolioPoint portfolio,
        List<BotDryRunPosition> positions,
        List<BotDryRunClosedTrade> closedTrades
) {
}
