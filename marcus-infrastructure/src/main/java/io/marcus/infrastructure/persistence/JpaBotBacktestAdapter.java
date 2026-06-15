package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.BotBacktestRun;
import io.marcus.domain.model.BotHistoricalClosedTrade;
import io.marcus.domain.model.BotDryRunPortfolioPoint;
import io.marcus.domain.port.BotBacktestPort;
import io.marcus.infrastructure.persistence.entity.BotBacktestRunEntity;
import io.marcus.infrastructure.persistence.entity.BotHistoricalClosedTradeEntity;
import io.marcus.infrastructure.persistence.entity.BotHistoricalPortfolioEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaBotBacktestAdapter implements BotBacktestPort {

    private static final String HISTORICAL = "HISTORICAL";

    private final SpringDataBotBacktestRunRepository runRepository;
    private final SpringDataBotHistoricalPortfolioRepository portfolioRepository;
    private final SpringDataBotHistoricalClosedTradeRepository closedTradeRepository;

    @Override
    @Transactional
    public BotBacktestRun saveRun(BotBacktestRun run, List<BotDryRunPortfolioPoint> equityHistory, List<BotHistoricalClosedTrade> closedTrades) {
        BotBacktestRunEntity runEntity = new BotBacktestRunEntity();
        runEntity.setRunId(run.runId());
        runEntity.setBotId(run.botId());
        runEntity.setRunName(run.runName());
        runEntity.setStartedAt(run.startedAt());
        runEntity.setEndedAt(run.endedAt());
        runEntity.setMetricsJson(run.metricsJson());
        BotBacktestRunEntity savedRun = runRepository.save(runEntity);

        List<BotHistoricalPortfolioEntity> portfolioEntities = equityHistory.stream()
                .map(point -> toPortfolioEntity(run.runId(), point))
                .toList();
        portfolioRepository.saveAll(portfolioEntities);

        List<BotHistoricalClosedTradeEntity> closedTradeEntities = closedTrades.stream()
                .map(this::toClosedTradeEntity)
                .toList();
        closedTradeRepository.saveAll(closedTradeEntities);

        return toRun(savedRun);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BotBacktestRun> findLatestRun(String botId) {
        return runRepository.findTopByBotIdOrderByCreatedAtDesc(botId).map(this::toRun);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BotDryRunPortfolioPoint> findPortfolioPoints(String botId, String runId) {
        return portfolioRepository.findByRunIdOrderByTimestampAsc(runId).stream()
                .map(this::toPortfolioPoint)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BotHistoricalClosedTrade> findClosedTrades(String botId, String runId) {
        return closedTradeRepository.findByRunIdOrderByExitTimestampAsc(runId).stream()
                .filter(entity -> botId.equals(entity.getBotId()))
                .map(this::toClosedTrade)
                .toList();
    }

    private BotHistoricalPortfolioEntity toPortfolioEntity(String runId, BotDryRunPortfolioPoint point) {
        BotHistoricalPortfolioEntity entity = new BotHistoricalPortfolioEntity();
        entity.setRunId(runId);
        entity.setBotId(point.botId());
        entity.setDataSource(HISTORICAL);
        entity.setTimestamp(point.timestamp());
        entity.setCash(point.cash());
        entity.setEquity(point.equity());
        entity.setRealizedPnl(point.realizedPnl());
        entity.setUnrealizedPnl(point.unrealizedPnl());
        entity.setTotalFees(point.totalFees());
        return entity;
    }

    private BotHistoricalClosedTradeEntity toClosedTradeEntity(BotHistoricalClosedTrade trade) {
        BotHistoricalClosedTradeEntity entity = new BotHistoricalClosedTradeEntity();
        entity.setRunId(trade.runId());
        entity.setBotId(trade.botId());
        entity.setTradeId(trade.tradeId());
        entity.setDataSource(HISTORICAL);
        entity.setSymbol(trade.symbol());
        entity.setMarketType(trade.marketType());
        entity.setSide(trade.side());
        entity.setQuantity(trade.quantity());
        entity.setEntryPrice(trade.entryPrice());
        entity.setExitPrice(trade.exitPrice());
        entity.setPnl(trade.pnl());
        entity.setFees(trade.fees());
        entity.setEntryTimestamp(trade.entryTimestamp());
        entity.setExitTimestamp(trade.exitTimestamp());
        entity.setDurationSeconds(trade.durationSeconds());
        return entity;
    }

    private BotDryRunPortfolioPoint toPortfolioPoint(BotHistoricalPortfolioEntity entity) {
        return new BotDryRunPortfolioPoint(
                entity.getBotId(),
                entity.getTimestamp(),
                entity.getCash(),
                entity.getEquity(),
                entity.getRealizedPnl(),
                entity.getUnrealizedPnl(),
                entity.getTotalFees()
        );
    }

    private BotBacktestRun toRun(BotBacktestRunEntity entity) {
        return new BotBacktestRun(
                entity.getRunId(),
                entity.getBotId(),
                entity.getRunName(),
                entity.getStartedAt(),
                entity.getEndedAt(),
                entity.getMetricsJson(),
                entity.getCreatedAt()
        );
    }

    private BotHistoricalClosedTrade toClosedTrade(BotHistoricalClosedTradeEntity entity) {
        return new BotHistoricalClosedTrade(
                entity.getRunId(),
                entity.getBotId(),
                entity.getTradeId(),
                entity.getSymbol(),
                entity.getMarketType(),
                entity.getSide(),
                entity.getQuantity(),
                entity.getEntryPrice(),
                entity.getExitPrice(),
                entity.getPnl(),
                entity.getFees(),
                entity.getEntryTimestamp(),
                entity.getExitTimestamp(),
                entity.getDurationSeconds()
        );
    }
}
