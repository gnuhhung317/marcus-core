package io.marcus.infrastructure.integration;

import io.marcus.domain.port.BotDiscoveryReadPort;
import io.marcus.domain.port.PortfolioReadPort.TimeSeriesPointSnapshot;
import io.marcus.domain.port.UserProfileReadPort.OffsetPaginationMetaSnapshot;
import io.marcus.domain.service.SignalMetricsCalculator;
import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.domain.vo.BotStatus;
import io.marcus.infrastructure.persistence.SpringDataBotRepository;
import io.marcus.infrastructure.persistence.SpringDataSignalRepository;
import io.marcus.infrastructure.persistence.SpringDataUserSubscriptionRepository;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import io.marcus.infrastructure.persistence.entity.SignalEntity;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StaticBotDiscoveryReadAdapter implements BotDiscoveryReadPort {

    private final SpringDataBotRepository springDataBotRepository;
    private final SpringDataSignalRepository springDataSignalRepository;
    private final SpringDataUserSubscriptionRepository springDataUserSubscriptionRepository;

    @Override
    @Transactional(readOnly = true)
    public BotDetailSnapshot getBotDetail(String botId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("botId must not be blank");
        }

        BotEntity bot = springDataBotRepository.findByBotIdWithExchange(botId)
                .filter(b -> b.getStatus() != BotStatus.DELETED)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found with id: " + botId));

        List<SignalEntity> signals = springDataSignalRepository.findByBotIdAndGeneratedTimestampIsNotNullOrderByGeneratedTimestampAsc(botId);

        SignalMetricsCalculator.MetricsResult metrics = calculateMetrics(signals);

        List<UserSubscriptionEntity> subscriptions = springDataUserSubscriptionRepository.findByBotIdAndStatusOrderByCreatedAtDesc(botId, SubscriptionStatus.ACTIVE);
        long subscriberCount = subscriptions.size();

        BotPerformanceSnapshot performance = new BotPerformanceSnapshot(
            metrics.annualReturn(),
            metrics.maxDrawdown(),
            metrics.sharpe(),
            metrics.winRate(),
            metrics.avgTradeReturn(),
            metrics.tradesPerDay()
        );

        return new BotDetailSnapshot(
                bot.getBotId(),
                bot.getName(),
                bot.getDescription(),
                bot.getStatus() != null ? bot.getStatus().name() : "INACTIVE",
                bot.getTradingPair(),
                resolveExchangeLabel(bot),
                bot.getDeveloperId(),
                bot.getApiKey(),
                bot.getCreatedAt(),
                bot.getUpdatedAt(),
                performance
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BotDiscoveryPageSnapshot listPublicBots(String q, String asset, String risk, String sort, int page, int size) {
        List<BotEntity> bots = springDataBotRepository.findAllWithExchange();
        
        List<BotDiscoverySnapshot> items = bots.stream()
                .filter(bot -> {
                    if (bot.getStatus() == BotStatus.DELETED) {
                        return false;
                    }
                    if (q != null && !q.isBlank()) {
                        String term = q.toLowerCase(Locale.ROOT);
                        boolean nameMatch = bot.getName() != null && bot.getName().toLowerCase(Locale.ROOT).contains(term);
                        boolean descMatch = bot.getDescription() != null && bot.getDescription().toLowerCase(Locale.ROOT).contains(term);
                        if (!nameMatch && !descMatch) return false;
                    }
                    if (asset != null && !asset.isBlank() && !asset.equalsIgnoreCase("ALL")) {
                        if (bot.getTradingPair() == null || !bot.getTradingPair().toLowerCase(Locale.ROOT).contains(asset.toLowerCase(Locale.ROOT))) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(bot -> {
                    List<SignalEntity> signals = springDataSignalRepository.findByBotIdAndGeneratedTimestampIsNotNullOrderByGeneratedTimestampAsc(bot.getBotId());
                    SignalMetricsCalculator.MetricsResult metrics = calculateMetrics(signals);
                    long subscribers = springDataUserSubscriptionRepository.findByBotIdAndStatusOrderByCreatedAtDesc(bot.getBotId(), SubscriptionStatus.ACTIVE).size();
                    return new BotDiscoverySnapshot(
                            bot.getBotId(),
                            bot.getName(),
                            bot.getDescription(),
                            bot.getTradingPair(),
                        metrics.risk(),
                        metrics.annualReturn(),
                        metrics.maxDrawdown(),
                            (int) subscribers
                    );
                })
                .toList();

        int totalElements = items.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<BotDiscoverySnapshot> pagedItems = items.subList(fromIndex, toIndex);

        OffsetPaginationMetaSnapshot meta = new OffsetPaginationMetaSnapshot(
                page,
                size,
                totalElements,
                totalPages,
                page < totalPages - 1
        );

        return new BotDiscoveryPageSnapshot(pagedItems, meta);
    }

    @Override
    public FavoriteStrategySnapshot favoriteStrategy(String userId, String strategyId) {
        return new FavoriteStrategySnapshot(strategyId, true);
    }

    @Override
    public StrategyDetailSnapshot getStrategyDetail(String strategyId) {
        return new StrategyDetailSnapshot(strategyId, "Alpha Trend Following", "Marcus Labs", "CRYPTO", "ACTIVE");
    }

    @Override
    public StrategyMetricsSnapshot getStrategyMetrics(String strategyId, String feeMode) {
        return new StrategyMetricsSnapshot(0.3842, -0.1245, 2.15, 2.84, 3.08, 1.62);
    }

    @Override
    public List<TimeSeriesPointSnapshot> listStrategyPerformanceSeries(String strategyId, String range) {
        LocalDateTime base = LocalDateTime.now().minusDays(30);
        return List.of(
                new TimeSeriesPointSnapshot(base, 10000.0),
                new TimeSeriesPointSnapshot(base.plusDays(5), 10250.0),
                new TimeSeriesPointSnapshot(base.plusDays(10), 10100.0),
                new TimeSeriesPointSnapshot(base.plusDays(15), 10450.0),
                new TimeSeriesPointSnapshot(base.plusDays(20), 10800.0),
                new TimeSeriesPointSnapshot(base.plusDays(25), 10720.0),
                new TimeSeriesPointSnapshot(base.plusDays(30), 11245.0)
        );
    }

    @Override
    public TradeLogPageSnapshot listStrategyTrades(String strategyId, int page, int size, String asset) {
        LocalDateTime now = LocalDateTime.now();
        List<TradeLogSnapshot> list = List.of(
                new TradeLogSnapshot(now.minusHours(4), "BTCUSDT", "BUY", 0.05, 68250.0, 0.0, 0.0),
                new TradeLogSnapshot(now.minusHours(12), "ETHUSDT", "SELL", 1.2, 3520.0, 3480.0, 48.0),
                new TradeLogSnapshot(now.minusDays(2), "SOLUSDT", "BUY", 15.0, 182.5, 189.2, 100.5)
        );
        return new TradeLogPageSnapshot(list, page, size, list.size());
    }

    @Override
    public LeaderboardStrategiesPageSnapshot listLeaderboardStrategies(
            String timeframe, String market, String asset, String rankMetric, int page, int size
    ) {
        List<LeaderboardStrategySnapshot> list = List.of(
                new LeaderboardStrategySnapshot(1, "strat_1", "Alpha Trend Following", "Marcus Labs", 0.3842, 2.15, -0.1245),
                new LeaderboardStrategySnapshot(2, "strat_2", "Mean Reversion Bot", "Quantify Inc", 0.2915, 1.84, -0.0950),
                new LeaderboardStrategySnapshot(3, "strat_3", "Arbitrage Scalper", "HFT Fund", 0.2240, 3.12, -0.0320)
        );
        return new LeaderboardStrategiesPageSnapshot(
                list,
                new OffsetPaginationMetaSnapshot(page, size, list.size(), 1, false)
        );
    }

    @Override
    public LeaderboardFeaturedSnapshot listLeaderboardFeatured() {
        return new LeaderboardFeaturedSnapshot(List.of(
                new LeaderboardFeaturedItemSnapshot("strat_1", "Alpha Trend Following", "HIGHEST_CAGR", 2.15),
                new LeaderboardFeaturedItemSnapshot("strat_3", "Arbitrage Scalper", "BEST_SHARPE", 3.12)
        ));
    }

    @Override
    public List<StrategySpotlightSnapshot> listLeaderboardSpotlights() {
        return List.of(
                new StrategySpotlightSnapshot("strat_1", "Alpha Trend Following", "CRYPTO", 0.0245),
                new StrategySpotlightSnapshot("strat_2", "Mean Reversion Bot", "CRYPTO", -0.0085),
                new StrategySpotlightSnapshot("strat_3", "Arbitrage Scalper", "CRYPTO", 0.0012)
        );
    }

    private SignalMetricsCalculator.SignalData toSignalData(SignalEntity signal) {
        return new SignalMetricsCalculator.SignalData(
                signal.getEntry(), signal.getTakeProfit(),
                signal.getStopLoss(), signal.getAction()
        );
    }

    private String resolveExchangeLabel(BotEntity bot) {
        if (bot.getExchange() != null) {
            if (bot.getExchange().getExchangeId() != null && !bot.getExchange().getExchangeId().isBlank()) {
                return bot.getExchange().getExchangeId().toUpperCase(Locale.ROOT);
            }
            if (bot.getExchange().getName() != null && !bot.getExchange().getName().isBlank()) {
                return bot.getExchange().getName().toUpperCase(Locale.ROOT);
            }
        }
        if (bot.getTradingPair() != null && !bot.getTradingPair().isBlank()) {
            return bot.getTradingPair().toUpperCase(Locale.ROOT);
        }
        return "UNASSIGNED";
    }

    private SignalMetricsCalculator.MetricsResult calculateMetrics(List<SignalEntity> signals) {
        if (signals == null || signals.isEmpty()) {
            return SignalMetricsCalculator.calculate(List.of(), 1);
        }

        List<SignalEntity> nonSimulatedSignals = signals.stream()
                .filter(s -> s.getMetadata() == null || !Boolean.TRUE.equals(s.getMetadata().get("simulation")))
                .toList();

        if (nonSimulatedSignals.isEmpty()) {
            return SignalMetricsCalculator.calculate(List.of(), 1);
        }

        List<SignalMetricsCalculator.SignalData> signalDataList = nonSimulatedSignals.stream()
                .map(this::toSignalData)
                .toList();

        LocalDateTime firstSignalAt = nonSimulatedSignals.stream()
                .map(SignalEntity::getGeneratedTimestamp)
                .filter(timestamp -> timestamp != null)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        LocalDateTime lastSignalAt = nonSimulatedSignals.stream()
                .map(SignalEntity::getGeneratedTimestamp)
                .filter(timestamp -> timestamp != null)
                .max(LocalDateTime::compareTo)
                .orElse(firstSignalAt);

        long ageDays = Math.max(1L, ChronoUnit.DAYS.between(firstSignalAt, lastSignalAt) + 1L);
        return SignalMetricsCalculator.calculate(signalDataList, ageDays);
    }
}
