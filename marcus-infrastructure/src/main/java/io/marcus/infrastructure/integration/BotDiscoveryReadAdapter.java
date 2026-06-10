package io.marcus.infrastructure.integration;

import io.marcus.domain.port.BotDiscoveryReadPort;
import io.marcus.domain.port.BotDiscoveryReadPort.BotDetailSnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.BotDiscoveryPageSnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.BotDiscoverySnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.BotPerformanceSnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.BotSpotlightSnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.FavoriteBotSnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.LeaderboardBotSnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.LeaderboardBotsPageSnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.LeaderboardFeaturedItemSnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.LeaderboardFeaturedSnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.TradeLogPageSnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.TradeLogSnapshot;
import io.marcus.domain.port.UserProfileReadPort.OffsetPaginationMetaSnapshot;
import io.marcus.domain.service.SignalMetricsCalculator;
import io.marcus.domain.vo.BotStatus;
import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.domain.vo.LeaderboardDataSource;
import io.marcus.domain.vo.LeaderboardRankMetric;
import io.marcus.infrastructure.persistence.SpringDataBotFavoriteRepository;
import io.marcus.infrastructure.persistence.SpringDataBotDryRunClosedTradeRepository;
import io.marcus.infrastructure.persistence.SpringDataBotHistoricalClosedTradeRepository;
import io.marcus.infrastructure.persistence.SpringDataBotRepository;
import io.marcus.infrastructure.persistence.SpringDataLeaderboardMetricsRepository;
import io.marcus.infrastructure.persistence.SpringDataSignalRepository;
import io.marcus.infrastructure.persistence.SpringDataUserRepository;
import io.marcus.infrastructure.persistence.SpringDataUserSubscriptionRepository;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import io.marcus.infrastructure.persistence.entity.BotFavoriteEntity;
import io.marcus.infrastructure.persistence.entity.BotHistoricalClosedTradeEntity;
import io.marcus.infrastructure.persistence.entity.BotLeaderboardMetricsEntity;
import io.marcus.infrastructure.persistence.entity.BotDryRunClosedTradeEntity;
import io.marcus.infrastructure.persistence.entity.SignalEntity;
import io.marcus.infrastructure.persistence.entity.UserEntity;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Real-time adapter for bot discovery and leaderboard operations.
 */
@Component
@Primary
@RequiredArgsConstructor
public class BotDiscoveryReadAdapter implements BotDiscoveryReadPort {

    private final SpringDataBotRepository springDataBotRepository;
    private final SpringDataSignalRepository springDataSignalRepository;
    private final SpringDataUserSubscriptionRepository springDataUserSubscriptionRepository;
    private final SpringDataUserRepository springDataUserRepository;
    private final SpringDataLeaderboardMetricsRepository leaderboardMetricsRepository;
    private final SpringDataBotDryRunClosedTradeRepository botDryRunClosedTradeRepository;
    private final SpringDataBotHistoricalClosedTradeRepository botHistoricalClosedTradeRepository;
    private final SpringDataBotFavoriteRepository botFavoriteRepository;

    @Override
    @Transactional(readOnly = true)
    public BotDetailSnapshot getBotDetail(String botId) {
        String normalizedBotId = requireBotId(botId);

        BotEntity bot = springDataBotRepository.findByBotIdWithExchange(normalizedBotId)
                .filter(b -> b.getStatus() != BotStatus.DELETED)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found with id: " + normalizedBotId));

        List<SignalEntity> signals = springDataSignalRepository
                .findByBotIdAndGeneratedTimestampIsNotNullOrderByGeneratedTimestampAsc(normalizedBotId);
        SignalMetricsCalculator.MetricsResult metrics = calculateMetrics(signals);

        BotPerformanceSnapshot performance = new BotPerformanceSnapshot(
                metrics.annualReturn(),
                metrics.maxDrawdown(),
                metrics.sharpe(),
                metrics.winRate(),
                metrics.avgTradeReturn(),
                metrics.tradesPerDay());

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
                performance);
    }

    @Override
    @Transactional(readOnly = true)
    public BotDiscoveryPageSnapshot listPublicBots(String q, String asset, String risk, String sort, int page,
            int size) {
        List<BotView> views = springDataBotRepository.findAllWithExchange().stream()
                .filter(bot -> bot.getStatus() != BotStatus.DELETED)
                .filter(bot -> matchesQuery(bot, q))
                .filter(bot -> matchesAsset(bot, asset))
                .map(bot -> toBotView(bot))
                .filter(view -> matchesRisk(view.metrics().risk(), risk))
                .toList();

        List<BotDiscoverySnapshot> items = views.stream()
                .map(this::toDiscoverySnapshot)
                .sorted(comparatorForSort(sort))
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
                page < totalPages - 1);

        return new BotDiscoveryPageSnapshot(pagedItems, meta);
    }

    @Override
    @Transactional
    public FavoriteBotSnapshot favoriteBot(String userId, String botId) {
        String normalizedUserId = requireUserId(userId);
        String normalizedBotId = requireBotId(botId);

        springDataUserRepository.findByUserId(normalizedUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + normalizedUserId));
        springDataBotRepository.findByBotId(normalizedBotId)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found with id: " + normalizedBotId));

        BotFavoriteEntity entity = botFavoriteRepository.findByUserIdAndBotId(normalizedUserId, normalizedBotId)
                .orElseGet(BotFavoriteEntity::new);
        entity.setUserId(normalizedUserId);
        entity.setBotId(normalizedBotId);
        botFavoriteRepository.save(entity);

        return new FavoriteBotSnapshot(normalizedBotId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public TradeLogPageSnapshot listBotTrades(String botId, int page, int size, String asset) {
        String normalizedBotId = requireBotId(botId);
        springDataBotRepository.findByBotId(normalizedBotId)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found with id: " + normalizedBotId));

        String normalizedAsset = normalizeAsset(asset);
        List<TradeView> trades = new ArrayList<>();
        trades.addAll(botDryRunClosedTradeRepository.findByBotIdOrderByExitTimestampAsc(normalizedBotId).stream()
                .map(this::toTradeView)
                .toList());
        trades.addAll(botHistoricalClosedTradeRepository.findByBotIdOrderByExitTimestampAsc(normalizedBotId).stream()
                .map(this::toTradeView)
                .toList());

        List<TradeLogSnapshot> filtered = trades.stream()
                .filter(trade -> normalizedAsset == null
                        || trade.assetPair().toUpperCase(Locale.ROOT).contains(normalizedAsset))
                .sorted(Comparator.comparing(TradeView::timestamp).reversed())
                .map(trade -> new TradeLogSnapshot(
                        trade.timestamp(),
                        trade.assetPair(),
                        trade.side(),
                        trade.size(),
                        trade.entryPrice(),
                        trade.exitPrice(),
                        trade.netPnl()))
                .toList();

        int totalElements = filtered.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<TradeLogSnapshot> pagedItems = filtered.subList(fromIndex, toIndex);
        return new TradeLogPageSnapshot(pagedItems, page, size, totalElements);
    }

    @Override
    @Transactional(readOnly = true)
    public LeaderboardBotsPageSnapshot listLeaderboardBots(
            LeaderboardDataSource dataSource, String market, String asset, LeaderboardRankMetric rankMetric, int page,
            int size) {
        String dataSourceStr = dataSource.name();
        String sortBy = rankMetric.name();
        long totalElementsLong = leaderboardMetricsRepository.countByDataSource(dataSourceStr);
        int limit = (int) Math.min(Integer.MAX_VALUE, totalElementsLong);

        List<LeaderboardBotSnapshot> snapshots = leaderboardMetricsRepository.findPaginated(
                dataSourceStr,
                sortBy,
                0,
                limit).stream()
                .map(metrics -> toLeaderboardSnapshot(metrics, market, asset))
                .filter(snapshot -> snapshot != null)
                .toList();

        int totalElements = snapshots.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<LeaderboardBotSnapshot> pagedItems = snapshots.subList(fromIndex, toIndex);

        return new LeaderboardBotsPageSnapshot(
                pagedItems,
                new OffsetPaginationMetaSnapshot(page, size, totalElements, totalPages, page < totalPages - 1));
    }

    @Override
    @Transactional(readOnly = true)
    public LeaderboardFeaturedSnapshot listLeaderboardFeatured() {
        LeaderboardBotsPageSnapshot bots = listLeaderboardBots(LeaderboardDataSource.DRY_RUN, null, null,
                LeaderboardRankMetric.CAGR, 0, 10);
        List<LeaderboardFeaturedItemSnapshot> featured = bots.items().stream()
                .map(s -> new LeaderboardFeaturedItemSnapshot(s.botId(), s.botName(), "HIGHEST_CAGR", s.sharpe()))
                .toList();
        return new LeaderboardFeaturedSnapshot(featured);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BotSpotlightSnapshot> listLeaderboardSpotlights() {
        LeaderboardBotsPageSnapshot bots = listLeaderboardBots(LeaderboardDataSource.DRY_RUN, null, null,
                LeaderboardRankMetric.CAGR, 0, 5);
        return bots.items().stream()
                .map(s -> new BotSpotlightSnapshot(s.botId(), s.botName(), "CRYPTO", s.cagr()))
                .toList();
    }

    private BotView toBotView(BotEntity bot) {
        List<SignalEntity> signals = springDataSignalRepository
                .findByBotIdAndGeneratedTimestampIsNotNullOrderByGeneratedTimestampAsc(bot.getBotId());
        SignalMetricsCalculator.MetricsResult metrics = calculateMetrics(signals);
        long subscribers = springDataUserSubscriptionRepository
                .findByBotIdAndStatusOrderByCreatedAtDesc(bot.getBotId(), SubscriptionStatus.ACTIVE).size();
        return new BotView(bot, metrics, subscribers);
    }

    private BotDiscoverySnapshot toDiscoverySnapshot(BotView view) {
        BotEntity bot = view.bot();
        SignalMetricsCalculator.MetricsResult metrics = view.metrics();
        return new BotDiscoverySnapshot(
                bot.getBotId(),
                bot.getName(),
                bot.getDescription(),
                bot.getTradingPair(),
                metrics.risk(),
                metrics.annualReturn(),
                metrics.maxDrawdown(),
                metrics.winRate(),
                (int) view.subscribers());
    }

    private LeaderboardBotSnapshot toLeaderboardSnapshot(BotLeaderboardMetricsEntity metrics, String market,
            String asset) {
        BotEntity bot = springDataBotRepository.findByBotId(metrics.getBotId())
                .orElse(null);
        if (bot == null || bot.getStatus() == BotStatus.DELETED) {
            return null;
        }
        if (!matchesMarket(bot, market) || !matchesAsset(bot, asset)) {
            return null;
        }

        String creatorName = springDataUserRepository.findByUserId(bot.getDeveloperId())
                .map(UserEntity::getUsername)
                .orElse("Unknown");
        return new LeaderboardBotSnapshot(
                0,
                bot.getBotId(),
                bot.getName(),
                creatorName,
                metrics.getCagr(),
                metrics.getSharpe(),
                metrics.getMaxDrawdown(),
                metrics.getDataSource());
    }

    private TradeView toTradeView(BotDryRunClosedTradeEntity entity) {
        return new TradeView(
                entity.getExitTimestamp(),
                entity.getSymbol(),
                entity.getSide(),
                entity.getQuantity().doubleValue(),
                entity.getEntryPrice().doubleValue(),
                entity.getExitPrice().doubleValue(),
                entity.getPnl().doubleValue());
    }

    private TradeView toTradeView(BotHistoricalClosedTradeEntity entity) {
        return new TradeView(
                entity.getExitTimestamp(),
                entity.getSymbol(),
                entity.getSide(),
                entity.getQuantity().doubleValue(),
                entity.getEntryPrice().doubleValue(),
                entity.getExitPrice().doubleValue(),
                entity.getPnl().doubleValue());
    }

    private Comparator<BotDiscoverySnapshot> comparatorForSort(String sort) {
        String normalizedSort = sort == null || sort.isBlank() ? "-return" : sort.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedSort) {
            case "return" -> Comparator.comparingDouble(BotDiscoverySnapshot::annualReturn)
                    .thenComparing(BotDiscoverySnapshot::botName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "drawdown" -> Comparator
                    .comparingDouble((BotDiscoverySnapshot snapshot) -> Math.abs(snapshot.maxDrawdown()))
                    .thenComparing(BotDiscoverySnapshot::botName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "-drawdown" -> Comparator.comparingDouble(BotDiscoverySnapshot::maxDrawdown).reversed()
                    .thenComparing(BotDiscoverySnapshot::botName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "subscribers" -> Comparator.comparingInt(BotDiscoverySnapshot::subscribers)
                    .thenComparing(BotDiscoverySnapshot::botName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "-subscribers" -> Comparator.comparingInt(BotDiscoverySnapshot::subscribers).reversed()
                    .thenComparing(BotDiscoverySnapshot::botName, Comparator.nullsLast(String::compareToIgnoreCase));
            default -> Comparator.comparingDouble(BotDiscoverySnapshot::annualReturn).reversed()
                    .thenComparing(BotDiscoverySnapshot::botName, Comparator.nullsLast(String::compareToIgnoreCase));
        };
    }

    private boolean matchesQuery(BotEntity bot, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }
        String term = q.toLowerCase(Locale.ROOT);
        boolean nameMatch = bot.getName() != null && bot.getName().toLowerCase(Locale.ROOT).contains(term);
        boolean descMatch = bot.getDescription() != null
                && bot.getDescription().toLowerCase(Locale.ROOT).contains(term);
        return nameMatch || descMatch;
    }

    private boolean matchesAsset(BotEntity bot, String asset) {
        if (asset == null || asset.isBlank() || asset.equalsIgnoreCase("ALL")) {
            return true;
        }
        String normalizedAsset = asset.toLowerCase(Locale.ROOT);
        if (bot.getTradingPair() != null && bot.getTradingPair().toLowerCase(Locale.ROOT).contains(normalizedAsset)) {
            return true;
        }
        if (bot.getAssetPairs() != null) {
            return bot.getAssetPairs().stream()
                    .filter(pair -> pair != null)
                    .anyMatch(pair -> pair.toLowerCase(Locale.ROOT).contains(normalizedAsset));
        }
        return false;
    }

    private boolean matchesMarket(BotEntity bot, String market) {
        if (market == null || market.isBlank() || market.equalsIgnoreCase("ALL")) {
            return true;
        }
        String normalizedMarket = market.toLowerCase(Locale.ROOT);
        if (bot.getExchange() != null) {
            String exchangeId = bot.getExchange().getExchangeId();
            String exchangeName = bot.getExchange().getName();
            if (exchangeId != null && exchangeId.toLowerCase(Locale.ROOT).contains(normalizedMarket)) {
                return true;
            }
            if (exchangeName != null && exchangeName.toLowerCase(Locale.ROOT).contains(normalizedMarket)) {
                return true;
            }
        }
        return bot.getTradingPair() != null && bot.getTradingPair().toLowerCase(Locale.ROOT).contains(normalizedMarket);
    }

    private boolean matchesRisk(String actualRisk, String requestedRisk) {
        if (requestedRisk == null || requestedRisk.isBlank() || requestedRisk.equalsIgnoreCase("ALL")) {
            return true;
        }
        return requestedRisk.equalsIgnoreCase(actualRisk);
    }

    private String normalizeAsset(String asset) {
        if (asset == null || asset.isBlank() || asset.equalsIgnoreCase("ALL")) {
            return null;
        }
        return asset.trim().toUpperCase(Locale.ROOT);
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

    private SignalMetricsCalculator.SignalData toSignalData(SignalEntity signal) {
        return new SignalMetricsCalculator.SignalData(
                signal.getEntry(), signal.getTakeProfit(),
                signal.getStopLoss(), signal.getAction());
    }

    private String requireBotId(String botId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("botId must not be blank");
        }
        return botId.trim();
    }

    private String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        return userId.trim();
    }

    private record BotView(BotEntity bot, SignalMetricsCalculator.MetricsResult metrics, long subscribers) {
    }

    private record TradeView(
            LocalDateTime timestamp,
            String assetPair,
            String side,
            double size,
            double entryPrice,
            double exitPrice,
            double netPnl) {
    }
}
