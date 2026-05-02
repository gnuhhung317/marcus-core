package io.marcus.infrastructure.integration;

import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.infrastructure.integration.demo.DemoTerminalReadAdapter;
import io.marcus.infrastructure.persistence.SpringDataBotRepository;
import io.marcus.infrastructure.persistence.SpringDataSignalRepository;
import io.marcus.infrastructure.persistence.SpringDataUserSubscriptionRepository;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import io.marcus.infrastructure.persistence.entity.SignalEntity;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Primary
@RequiredArgsConstructor
public class StaticTerminalReadAdapter implements TerminalReadPort {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 1, 9, 0);

    private final SpringDataBotRepository springDataBotRepository;
    private final SpringDataSignalRepository springDataSignalRepository;
    private final SpringDataUserSubscriptionRepository springDataUserSubscriptionRepository;
    private final DemoTerminalReadAdapter demoTerminalReadAdapter;

    @Override
    @Transactional(readOnly = true)
    public BotDetailSnapshot getBotDetail(String botId) {
        String normalizedBotId = normalize(botId, "bot-demo-001");
        BotEntity bot = springDataBotRepository.findByBotIdWithExchange(normalizedBotId)
                .orElseThrow(() -> new NoSuchElementException("Bot not found: " + normalizedBotId));

        BotMetrics metrics = calculateBotMetrics(normalizedBotId);
        return new BotDetailSnapshot(
                normalizedBotId,
                bot.getName(),
                bot.getDescription(),
                bot.getStatus() != null ? bot.getStatus().name() : null,
                bot.getTradingPair(),
                resolveExchangeId(bot),
                bot.getDeveloperId(),
                bot.getApiKey(),
                bot.getCreatedAt(),
                bot.getUpdatedAt(),
                new BotPerformanceSnapshot(
                        metrics.annualReturn(),
                        metrics.maxDrawdown(),
                        metrics.sharpe(),
                        metrics.winRate(),
                        metrics.avgTradeReturn(),
                        metrics.tradesPerDay()
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    public BotDiscoveryPageSnapshot listPublicBots(String q, String asset, String risk, String sort, int page, int size) {
        String normalizedQuery = normalize(q, null);
        String normalizedAsset = normalize(asset, null);
        String normalizedRisk = normalize(risk, "ALL").toUpperCase(Locale.ROOT);
        String normalizedSort = normalize(sort, "-return").toLowerCase(Locale.ROOT);

        Map<String, Long> subscribersByBotId = springDataUserSubscriptionRepository
                .findAll()
                .stream()
                .filter(entity -> entity.getStatus() == SubscriptionStatus.ACTIVE)
                .collect(Collectors.groupingBy(UserSubscriptionEntity::getBotId, Collectors.counting()));

        Map<String, List<SignalEntity>> signalsByBotId = springDataSignalRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(SignalEntity::getBotId));

        List<BotDiscoverySnapshot> filtered = springDataBotRepository.findAllWithExchange()
                .stream()
                .map(bot -> toDiscoverySnapshot(
                        bot,
                        subscribersByBotId.getOrDefault(bot.getBotId(), 0L),
                        calculateBotMetrics(
                                bot.getBotId(),
                                signalsByBotId.getOrDefault(bot.getBotId(), List.of()),
                                subscribersByBotId.getOrDefault(bot.getBotId(), 0L)
                        )
                ))
                .filter(snapshot -> normalizedQuery == null || matchesQuery(snapshot, normalizedQuery))
                .filter(snapshot -> normalizedAsset == null || snapshot.asset().equalsIgnoreCase(normalizedAsset))
                .filter(snapshot -> "ALL".equals(normalizedRisk) || snapshot.risk().equalsIgnoreCase(normalizedRisk))
                .sorted(comparatorForDiscoverySnapshot(normalizedSort))
                .toList();

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 100));
        int fromIndex = Math.min(normalizedPage * normalizedSize, filtered.size());
        int toIndex = Math.min(fromIndex + normalizedSize, filtered.size());
        List<BotDiscoverySnapshot> items = filtered.subList(fromIndex, toIndex);

        long totalElements = filtered.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil(totalElements / (double) normalizedSize);
        OffsetPaginationMetaSnapshot meta = new OffsetPaginationMetaSnapshot(
                normalizedPage,
                normalizedSize,
                totalElements,
                totalPages,
                normalizedPage + 1 < totalPages
        );
        return new BotDiscoveryPageSnapshot(items, meta);
    }

    @Override
    public FavoriteStrategySnapshot favoriteStrategy(String userId, String strategyId) {
        return demoTerminalReadAdapter.favoriteStrategy(userId, strategyId);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewSnapshot getDashboardOverview(String userId) {
        String normalizedUserId = normalize(userId, "user-demo-001");
        List<UserSubscriptionEntity> activeSubscriptions = springDataUserSubscriptionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(normalizedUserId, SubscriptionStatus.ACTIVE);
        List<SignalEntity> relatedSignals = springDataSignalRepository.findAll()
                .stream()
                .filter(signal -> activeSubscriptions.stream()
                        .anyMatch(subscription -> Objects.equals(subscription.getBotId(), signal.getBotId())))
                .toList();

        double score = relatedSignals.stream().mapToDouble(this::deriveSignalReturn).sum();
        long successfulSignals = relatedSignals.stream()
                .filter(signal -> deriveSignalReturn(signal) > 0)
                .count();

        double totalEquity = round2(10_000.0 + activeSubscriptions.size() * 250.0 + score * 1_000.0);
        double openPnl = round2(score * 1_000.0);
        double winRate = relatedSignals.isEmpty() ? 0.0 : round4(successfulSignals / (double) relatedSignals.size());
        int activeBots = activeSubscriptions.size();
        return new DashboardOverviewSnapshot(totalEquity, openPnl, winRate, activeBots);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSeriesPointSnapshot> listDashboardEquitySeries(String userId, String range) {
        String normalizedUserId = normalize(userId, "user-demo-001");
        String normalizedRange = normalize(range, "1M").toUpperCase(Locale.ROOT);
        int points = pointsForRange(normalizedRange);

        List<String> subscribedBotIds = springDataUserSubscriptionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(normalizedUserId, SubscriptionStatus.ACTIVE)
                .stream()
                .map(UserSubscriptionEntity::getBotId)
                .toList();

        List<SignalEntity> orderedSignals = springDataSignalRepository.findAll()
                .stream()
                .filter(signal -> subscribedBotIds.contains(signal.getBotId()))
                .filter(signal -> signal.getGeneratedTimestamp() != null)
                .sorted(Comparator.comparing(SignalEntity::getGeneratedTimestamp))
                .toList();

        if (orderedSignals.isEmpty()) {
            return List.of();
        }

        int startIndex = Math.max(0, orderedSignals.size() - points);
        List<SignalEntity> window = orderedSignals.subList(startIndex, orderedSignals.size());
        List<TimeSeriesPointSnapshot> result = new ArrayList<>(window.size());
        double cumulative = 0.0;
        for (SignalEntity signal : window) {
            cumulative += deriveSignalReturn(signal);
            result.add(new TimeSeriesPointSnapshot(signal.getGeneratedTimestamp(), round4(10_000.0 + cumulative * 1_000.0)));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeAllocationSnapshot> listExchangeAllocation(String userId) {
        String normalizedUserId = normalize(userId, "user-demo-001");
        Map<String, String> exchangeByBotId = springDataBotRepository.findAllWithExchange()
                .stream()
                .collect(Collectors.toMap(
                        BotEntity::getBotId,
                        this::resolveExchangeLabel,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<UserSubscriptionEntity> activeSubscriptions = springDataUserSubscriptionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(normalizedUserId, SubscriptionStatus.ACTIVE);

        Map<String, Long> countsByExchange = activeSubscriptions.stream()
                .collect(Collectors.groupingBy(
                        subscription -> exchangeByBotId.getOrDefault(subscription.getBotId(), "UNASSIGNED"),
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        long total = countsByExchange.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0L) {
            return List.of();
        }

        return countsByExchange.entrySet().stream()
                .map(entry -> new ExchangeAllocationSnapshot(entry.getKey(), round2(entry.getValue() / (double) total)))
                .sorted((left, right) -> Double.compare(right.percentage(), left.percentage()))
                .toList();
    }

    @Override
    public StrategyDetailSnapshot getStrategyDetail(String strategyId) {
        return demoTerminalReadAdapter.getStrategyDetail(strategyId);
    }

    @Override
    public StrategyMetricsSnapshot getStrategyMetrics(String strategyId, String feeMode) {
        return demoTerminalReadAdapter.getStrategyMetrics(strategyId, feeMode);
    }

    @Override
    public List<TimeSeriesPointSnapshot> listStrategyPerformanceSeries(String strategyId, String range) {
        return demoTerminalReadAdapter.listStrategyPerformanceSeries(strategyId, range);
    }

    @Override
    public TradeLogPageSnapshot listStrategyTrades(String strategyId, int page, int size, String asset) {
        return demoTerminalReadAdapter.listStrategyTrades(strategyId, page, size, asset);
    }

    @Override
    public LeaderboardStrategiesPageSnapshot listLeaderboardStrategies(
            String timeframe,
            String market,
            String asset,
            String rankMetric,
            int page,
            int size
    ) {
        return demoTerminalReadAdapter.listLeaderboardStrategies(timeframe, market, asset, rankMetric, page, size);
    }

    @Override
    public LeaderboardFeaturedSnapshot listLeaderboardFeatured() {
        return demoTerminalReadAdapter.listLeaderboardFeatured();
    }

    @Override
    public List<StrategySpotlightSnapshot> listLeaderboardSpotlights() {
        return demoTerminalReadAdapter.listLeaderboardSpotlights();
    }

    @Override
    public PaperSessionSummarySnapshot getPaperSessionSummary(String userId) {
        return demoTerminalReadAdapter.getPaperSessionSummary(userId);
    }

    @Override
    public List<PaperSignalSnapshot> listPaperSignals(String status, int limit) {
        return demoTerminalReadAdapter.listPaperSignals(status, limit);
    }

    @Override
    public PaperExecutionLogPageSnapshot listPaperExecutionLogs(String userId, String cursor, int limit) {
        return demoTerminalReadAdapter.listPaperExecutionLogs(userId, cursor, limit);
    }

    @Override
    public PaperOrderSnapshot createPaperOrder(String userId, PaperOrderCreateSnapshot request) {
        return demoTerminalReadAdapter.createPaperOrder(userId, request);
    }

    @Override
    public PaperSessionStateSnapshot pausePaperSession(String userId) {
        return demoTerminalReadAdapter.pausePaperSession(userId);
    }

    @Override
    public PaperSessionStateSnapshot resumePaperSession(String userId) {
        return demoTerminalReadAdapter.resumePaperSession(userId);
    }

    @Override
    public UserProfileSnapshot getCurrentUserProfile(String userId) {
        return demoTerminalReadAdapter.getCurrentUserProfile(userId);
    }

    @Override
    public UserPreferencesSnapshot updateCurrentUserPreferences(String userId, UserPreferencesUpdateSnapshot request) {
        return demoTerminalReadAdapter.updateCurrentUserPreferences(userId, request);
    }

    @Override
    public List<ApiKeySummarySnapshot> listCurrentUserApiKeys(String userId) {
        return demoTerminalReadAdapter.listCurrentUserApiKeys(userId);
    }

    @Override
    public CreateApiKeySnapshot createCurrentUserApiKey(String userId, String label) {
        return demoTerminalReadAdapter.createCurrentUserApiKey(userId, label);
    }

    @Override
    public void deleteCurrentUserApiKey(String userId, String apiKeyId) {
        demoTerminalReadAdapter.deleteCurrentUserApiKey(userId, apiKeyId);
    }

    @Override
    public LoginActivityPageSnapshot listCurrentUserLoginActivities(String userId, int page, int size) {
        return demoTerminalReadAdapter.listCurrentUserLoginActivities(userId, page, size);
    }

    @Override
    public List<SignalItemSnapshot> listSignals(String status, int limit) {
        return demoTerminalReadAdapter.listSignals(status, limit);
    }

    @Override
    public ConnectivityHealthSnapshot getSystemConnectivityHealth() {
        return demoTerminalReadAdapter.getSystemConnectivityHealth();
    }

    @Override
    public ExecutionLogPageSnapshot listSystemExecutionLogs(String cursor, int limit) {
        return demoTerminalReadAdapter.listSystemExecutionLogs(cursor, limit);
    }

    private BotDiscoverySnapshot toDiscoverySnapshot(BotEntity bot, long subscribers, BotMetrics metrics) {
        return new BotDiscoverySnapshot(
                bot.getBotId(),
                bot.getName(),
                bot.getDescription(),
                bot.getTradingPair(),
                deriveRisk(metrics),
                metrics.annualReturn(),
                metrics.maxDrawdown(),
                (int) subscribers
        );
    }

    private Comparator<BotDiscoverySnapshot> comparatorForDiscoverySnapshot(String sort) {
        return switch (sort) {
            case "return" -> Comparator.comparingDouble(BotDiscoverySnapshot::annualReturn);
            case "-return" -> Comparator.comparingDouble(BotDiscoverySnapshot::annualReturn).reversed();
            case "drawdown" -> Comparator.comparingDouble(BotDiscoverySnapshot::maxDrawdown);
            case "-drawdown" -> Comparator.comparingDouble(BotDiscoverySnapshot::maxDrawdown).reversed();
            case "subscribers" -> Comparator.comparingInt(BotDiscoverySnapshot::subscribers);
            case "-subscribers" -> Comparator.comparingInt(BotDiscoverySnapshot::subscribers).reversed();
            default -> throw new IllegalArgumentException("Unsupported sort: " + sort);
        };
    }

    private boolean matchesQuery(BotDiscoverySnapshot snapshot, String query) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        return snapshot.botId().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || snapshot.botName().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || snapshot.description().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || snapshot.asset().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || snapshot.risk().toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private BotMetrics calculateBotMetrics(String botId) {
        List<SignalEntity> signals = springDataSignalRepository.findAll().stream()
                .filter(signal -> Objects.equals(signal.getBotId(), botId))
                .toList();
        long subscribers = springDataUserSubscriptionRepository.findAll().stream()
                .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
                .filter(subscription -> Objects.equals(subscription.getBotId(), botId))
                .count();
        return calculateBotMetrics(botId, signals, subscribers);
    }

    private BotMetrics calculateBotMetrics(String botId, List<SignalEntity> signals, long subscribers) {
        if (signals == null || signals.isEmpty()) {
            return new BotMetrics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, deriveRisk(0.0, 0.0), subscribers);
        }

        double annualReturn = round4(signals.stream().mapToDouble(this::deriveSignalReturn).average().orElse(0.0));
        double maxDrawdown = round4(signals.stream().mapToDouble(this::deriveSignalDrawdown).max().orElse(0.0));
        long profitableSignals = signals.stream().filter(signal -> deriveSignalReturn(signal) > 0).count();
        double winRate = round4(profitableSignals / (double) signals.size());
        double avgTradeReturn = round4(signals.stream().mapToDouble(this::deriveSignalReturn).average().orElse(0.0));
        double tradesPerDay = round4(signals.size() / Math.max(1.0, calculateAgeDays(signals)));
        double sharpe = round4(annualReturn / Math.max(0.01, maxDrawdown + 0.01));
        return new BotMetrics(annualReturn, maxDrawdown, sharpe, winRate, avgTradeReturn, tradesPerDay, deriveRisk(annualReturn, maxDrawdown), subscribers);
    }

    private double calculateAgeDays(List<SignalEntity> signals) {
        LocalDateTime earliest = signals.stream()
                .map(SignalEntity::getGeneratedTimestamp)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(BASE_TIME);
        LocalDateTime latest = signals.stream()
                .map(SignalEntity::getGeneratedTimestamp)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(BASE_TIME.plusDays(1));
        long days = java.time.Duration.between(earliest, latest).toDays();
        return Math.max(days, 1L);
    }

    private double deriveSignalReturn(SignalEntity signal) {
        double entry = toDouble(signal.getEntry());
        if (entry == 0.0d) {
            return 0.0d;
        }

        double referencePrice = toDouble(signal.getTakeProfit());
        if (referencePrice == 0.0d) {
            referencePrice = toDouble(signal.getStopLoss());
        }
        if (referencePrice == 0.0d) {
            return 0.0d;
        }

        double direction = switch (signal.getAction()) {
            case OPEN_SHORT, CLOSE_SHORT -> -1.0d;
            default -> 1.0d;
        };
        return round4(((referencePrice - entry) / Math.abs(entry)) * direction);
    }

    private double deriveSignalDrawdown(SignalEntity signal) {
        double entry = toDouble(signal.getEntry());
        double stopLoss = toDouble(signal.getStopLoss());
        if (entry == 0.0d || stopLoss == 0.0d) {
            return 0.0d;
        }
        return round4(Math.max(0.0d, Math.abs(entry - stopLoss) / Math.abs(entry)));
    }

    private String deriveRisk(double annualReturn, double maxDrawdown) {
        if (maxDrawdown >= 0.25d || annualReturn <= 0.05d) {
            return "HIGH";
        }
        if (maxDrawdown >= 0.12d) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String deriveRisk(BotMetrics metrics) {
        return deriveRisk(metrics.annualReturn(), metrics.maxDrawdown());
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

    private String resolveExchangeId(BotEntity bot) {
        return resolveExchangeLabel(bot);
    }

    private int pointsForRange(String range) {
        return switch (range) {
            case "1D" -> 24;
            case "1W" -> 7;
            case "1M" -> 30;
            case "YTD" -> 24;
            case "ALL" -> 36;
            default -> 30;
        };
    }

    private double toDouble(BigDecimal value) {
        return value == null ? 0.0d : value.doubleValue();
    }

    private record BotMetrics(
            double annualReturn,
            double maxDrawdown,
            double sharpe,
            double winRate,
            double avgTradeReturn,
            double tradesPerDay,
            String risk,
            long subscribers
    ) {
    }

    private double round4(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
