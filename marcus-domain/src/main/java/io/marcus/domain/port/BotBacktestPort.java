package io.marcus.domain.port;

import io.marcus.domain.model.BotBacktestRun;
import io.marcus.domain.model.BotHistoricalClosedTrade;
import io.marcus.domain.model.BotDryRunPortfolioPoint;

import java.util.List;
import java.util.Optional;

public interface BotBacktestPort {

    BotBacktestRun saveRun(BotBacktestRun run, List<BotDryRunPortfolioPoint> equityHistory, List<BotHistoricalClosedTrade> closedTrades);

    Optional<BotBacktestRun> findLatestRun(String botId);

    List<BotDryRunPortfolioPoint> findPortfolioPoints(String botId, String runId);

    List<BotHistoricalClosedTrade> findClosedTrades(String botId, String runId);
}
