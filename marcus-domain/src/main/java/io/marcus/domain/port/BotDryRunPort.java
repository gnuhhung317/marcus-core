package io.marcus.domain.port;

import io.marcus.domain.model.BotDryRunPortfolioPoint;
import io.marcus.domain.model.BotDryRunState;

import java.util.List;
import java.util.Optional;

public interface BotDryRunPort {

    BotDryRunState syncSnapshot(BotDryRunState state);

    Optional<BotDryRunState> findLatestState(String botId);

    List<BotDryRunPortfolioPoint> findPortfolioPoints(String botId);
}
