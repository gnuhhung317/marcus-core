package io.marcus.infrastructure.integration.demo;

import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@RequiredArgsConstructor
public class DemoTerminalReadAdapter implements TerminalReadPort {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 1, 9, 0);
    private final ConcurrentMap<String, String> paperSessionStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, java.util.Set<String>> favoriteStrategiesByUser = new ConcurrentHashMap<>();

    @Override
    public BotDetailSnapshot getBotDetail(String botId) {
        throw new UnsupportedOperationException("Demo adapter only supports Phase 2+ methods");
    }

    @Override
    public BotDiscoveryPageSnapshot listPublicBots(String q, String asset, String risk, String sort, int page, int size) {
        throw new UnsupportedOperationException("Demo adapter only supports Phase 2+ methods");
    }

    @Override
    public FavoriteStrategySnapshot favoriteStrategy(String userId, String strategyId) {
        String normalizedUserId = normalize(userId, "user-demo-001");
        String normalizedStrategyId = normalize(strategyId, "strat-demo-001");

        favoriteStrategiesByUser
                .computeIfAbsent(normalizedUserId, ignored -> ConcurrentHashMap.newKeySet())
                .add(normalizedStrategyId);

        return new FavoriteStrategySnapshot(normalizedStrategyId, true);
    }

    @Override
    public DashboardOverviewSnapshot getDashboardOverview(String userId) {
        throw new UnsupportedOperationException("Demo adapter only supports Phase 2+ methods");
    }

    @Override
    public List<TimeSeriesPointSnapshot> listDashboardEquitySeries(String userId, String range) {
        throw new UnsupportedOperationException("Demo adapter only supports Phase 2+ methods");
    }

    @Override
    public List<ExchangeAllocationSnapshot> listExchangeAllocation(String userId) {
        throw new UnsupportedOperationException("Demo adapter only supports Phase 2+ methods");
    }

    @Override
    public StrategyDetailSnapshot getStrategyDetail(String strategyId) {
        String normalizedStrategyId = normalize(strategyId, "strat-demo-001");
        String code = shortCode(normalizedStrategyId);

        return new StrategyDetailSnapshot(
                normalizedStrategyId,
                "Strategy " + code.toUpperCase(Locale.ROOT),
                "Marcus Labs",
                marketFromSeed(normalizedStrategyId),
                "ACTIVE"
        );
    }

    @Override
    public StrategyMetricsSnapshot getStrategyMetrics(String strategyId, String feeMode) {
        String normalizedStrategyId = normalize(strategyId, "strat-demo-001");
        boolean afterFees = "AFTER_FEES".equalsIgnoreCase(normalize(feeMode, "RAW"));

        double feePenalty = afterFees ? 0.92 : 1.0;
        return new StrategyMetricsSnapshot(
                round4(scaled(normalizedStrategyId + ":annual", 0.12, 0.74) * feePenalty),
                scaled(normalizedStrategyId + ":drawdown", 0.02, 0.28),
                round4(scaled(normalizedStrategyId + ":sharpe", 0.60, 2.60) * feePenalty),
                round4(scaled(normalizedStrategyId + ":sortino", 0.90, 3.40) * feePenalty),
                round4(scaled(normalizedStrategyId + ":calmar", 0.50, 2.10) * feePenalty),
                round4(scaled(normalizedStrategyId + ":pf", 1.05, 3.25) * feePenalty)
        );
    }

    @Override
    public List<TimeSeriesPointSnapshot> listStrategyPerformanceSeries(String strategyId, String range) {
        String normalizedStrategyId = normalize(strategyId, "strat-demo-001");
        String normalizedRange = normalize(range, "1M").toUpperCase(Locale.ROOT);

        int points = switch (normalizedRange) {
            case "1D" -> 24;
            case "1W" -> 7;
            case "1M" -> 30;
            case "YTD" -> 24;
            case "ALL" -> 36;
            default -> 30;
        };

        double baseValue = scaled(normalizedStrategyId + ":series-base", 90.0, 135.0);
        List<TimeSeriesPointSnapshot> result = new ArrayList<>(points);
        for (int index = 0; index < points; index++) {
            double drift = scaled(normalizedStrategyId + ":series-drift:" + index, -1.20, 2.20);
            double wave = Math.sin((index + 1) / 3.0) * 0.85;
            baseValue = round4(baseValue + drift * 0.25 + wave * 0.15);
            result.add(new TimeSeriesPointSnapshot(seriesTimestamp(points, index, normalizedRange), baseValue));
        }
        return result;
    }

    @Override
    public TradeLogPageSnapshot listStrategyTrades(String strategyId, int page, int size, String asset) {
        String normalizedStrategyId = normalize(strategyId, "strat-demo-001");
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 100));
        String assetPair = normalize(asset, pairFromSeed(normalizedStrategyId));

        List<TradeLogSnapshot> items = new ArrayList<>(normalizedSize);
        int start = normalizedPage * normalizedSize;
        for (int index = 0; index < normalizedSize; index++) {
            int absoluteIndex = start + index;
            boolean longSide = (absoluteIndex % 2) == 0;
            double entry = scaled(normalizedStrategyId + ":entry:" + absoluteIndex, 20.0, 180.0);
            double exit = entry + scaled(normalizedStrategyId + ":exit:" + absoluteIndex, -8.0, 12.0);
            double amount = scaled(normalizedStrategyId + ":size:" + absoluteIndex, 0.2, 4.5);
            double pnl = round4((exit - entry) * amount * (longSide ? 1.0 : -1.0));

            items.add(new TradeLogSnapshot(
                    timeAt(normalizedStrategyId + ":trade:" + absoluteIndex, -absoluteIndex - 1),
                    assetPair,
                    longSide ? "LONG" : "SHORT",
                    amount,
                    round4(entry),
                    round4(exit),
                    pnl
            ));
        }

        return new TradeLogPageSnapshot(items, normalizedPage, normalizedSize, 320L);
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
        String timeframeValue = normalize(timeframe, "ALL").toUpperCase(Locale.ROOT);
        String marketValue = normalize(market, "CRYPTO").toUpperCase(Locale.ROOT);
        String assetValue = normalize(asset, "BTCUSDT").toUpperCase(Locale.ROOT);
        String rankMetricValue = normalize(rankMetric, "sharpe").toLowerCase(Locale.ROOT);

        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 100));

        List<LeaderboardStrategySnapshot> items = new ArrayList<>(normalizedSize);
        int startRank = normalizedPage * normalizedSize + 1;
        for (int index = 0; index < normalizedSize; index++) {
            int rank = startRank + index;
            String strategyId = "strat-" + timeframeValue.toLowerCase(Locale.ROOT) + "-" + rank;
            double sharpe = scaled(strategyId + ":sharpe", 0.70, 2.80);
            double cagr = scaled(strategyId + ":cagr", 0.10, 0.85);
            double maxDrawdown = scaled(strategyId + ":mdd", 0.03, 0.32);

            items.add(new LeaderboardStrategySnapshot(
                    rank,
                    strategyId,
                    assetValue + " Momentum " + rankMetricValue.toUpperCase(Locale.ROOT) + " " + rank,
                    marketValue + " Desk",
                    cagr,
                    sharpe,
                    maxDrawdown
            ));
        }

        long totalElements = 500L;
        int totalPages = (int) Math.ceil(totalElements / (double) normalizedSize);
        OffsetPaginationMetaSnapshot meta = new OffsetPaginationMetaSnapshot(
                normalizedPage,
                normalizedSize,
                totalElements,
                totalPages,
                normalizedPage + 1 < totalPages
        );
        return new LeaderboardStrategiesPageSnapshot(items, meta);
    }

    @Override
    public LeaderboardFeaturedSnapshot listLeaderboardFeatured() {
        List<LeaderboardFeaturedItemSnapshot> items = List.of(
                new LeaderboardFeaturedItemSnapshot("strat-featured-1", "Apex Trend", "TOP 1", 2.41),
                new LeaderboardFeaturedItemSnapshot("strat-featured-2", "Quantum Mean", "TOP 2", 2.08),
                new LeaderboardFeaturedItemSnapshot("strat-featured-3", "Sigma Breakout", "TOP 3", 1.97)
        );
        return new LeaderboardFeaturedSnapshot(items);
    }

    @Override
    public List<StrategySpotlightSnapshot> listLeaderboardSpotlights() {
        return List.of(
                new StrategySpotlightSnapshot("strat-spotlight-1", "Neutron Drift", "CRYPTO", 0.0312),
                new StrategySpotlightSnapshot("strat-spotlight-2", "FX Pulse", "FOREX", 0.0185),
                new StrategySpotlightSnapshot("strat-spotlight-3", "Commod Scan", "COMMODITIES", -0.0074)
        );
    }

    @Override
    public PaperSessionSummarySnapshot getPaperSessionSummary(String userId) {
        String normalizedUserId = normalize(userId, "user-demo-001");
        String currentState = paperSessionStates.computeIfAbsent(normalizedUserId, ignored -> "RUNNING");
        double virtualBalance = scaled(normalizedUserId + ":paper-balance", 5_000.0, 50_000.0);
        double openPnl = scaled(normalizedUserId + ":paper-pnl", -850.0, 1_950.0);
        double buyingPower = round2(virtualBalance * scaled(normalizedUserId + ":paper-power", 0.45, 0.88));

        return new PaperSessionSummarySnapshot(
                "ps-" + shortCode(normalizedUserId),
                currentState,
                virtualBalance,
                openPnl,
                buyingPower
        );
    }

    @Override
    public List<PaperSignalSnapshot> listPaperSignals(String status, int limit) {
        String normalizedStatus = normalize(status, "ALL").toUpperCase(Locale.ROOT);
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        List<String> statuses = normalizedStatus.equals("ALL")
                ? List.of("ACTIVE", "EXECUTED", "EXPIRED")
                : List.of(normalizedStatus);

        List<PaperSignalSnapshot> items = new ArrayList<>(normalizedLimit);
        for (int index = 0; index < normalizedLimit; index++) {
            String statusValue = statuses.get(index % statuses.size());
            String signalId = "paper-sig-" + (index + 1);
            String botId = "bot-paper-" + ((index % 4) + 1);

            items.add(new PaperSignalSnapshot(
                    signalId,
                    botId,
                    pairFromSeed(signalId),
                    (index % 2 == 0) ? "BUY" : "SELL",
                    scaled(signalId + ":confidence", 0.50, 0.99),
                    statusValue,
                    timeAt(signalId, -index)
            ));
        }

        return items;
    }

    @Override
    public PaperExecutionLogPageSnapshot listPaperExecutionLogs(String userId, String cursor, int limit) {
        String normalizedUserId = normalize(userId, "user-demo-001");
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        int offset = decodeCursorOffset(cursor);

        List<PaperExecutionLogItemSnapshot> items = new ArrayList<>(normalizedLimit);
        for (int index = 0; index < normalizedLimit; index++) {
            int absoluteIndex = offset + index;
            String seed = normalizedUserId + ":paper-log:" + absoluteIndex;
            items.add(new PaperExecutionLogItemSnapshot(
                    timeAt(seed, -absoluteIndex),
                    (absoluteIndex % 10 == 0) ? "WARN" : "INFO",
                    "Paper execution event #" + boundedInt(seed, 1000, 9999)
            ));
        }

        int nextOffset = offset + normalizedLimit;
        boolean hasMore = nextOffset < 1_000;
        CursorPaginationMetaSnapshot meta = new CursorPaginationMetaSnapshot(
                cursor,
                hasMore ? "paper-cursor-" + nextOffset : null,
                normalizedLimit,
                hasMore
        );
        return new PaperExecutionLogPageSnapshot(items, meta);
    }

    @Override
    public PaperOrderSnapshot createPaperOrder(String userId, PaperOrderCreateSnapshot request) {
        String normalizedUserId = normalize(userId, "user-demo-001");
        String seed = normalizedUserId
                + ":"
                + request.assetPair()
                + ":"
                + request.orderType()
                + ":"
                + request.side()
                + ":"
                + request.quantity();

        double executedPrice;
        if ("LIMIT".equalsIgnoreCase(request.orderType())) {
            Double limitPrice = request.limitPrice();
            executedPrice = round4(limitPrice == null ? 0.0 : limitPrice.doubleValue());
        } else {
            executedPrice = scaled(seed + ":executed", 20.0, 180.0);
        }

        return new PaperOrderSnapshot(
                "ord_" + maskedBlock(seed, 8),
                "ACCEPTED",
                executedPrice
        );
    }

    @Override
    public PaperSessionStateSnapshot pausePaperSession(String userId) {
        String normalizedUserId = normalize(userId, "user-demo-001");
        paperSessionStates.put(normalizedUserId, "PAUSED");
        return new PaperSessionStateSnapshot("ps-" + shortCode(normalizedUserId), "PAUSED");
    }

    @Override
    public PaperSessionStateSnapshot resumePaperSession(String userId) {
        String normalizedUserId = normalize(userId, "user-demo-001");
        paperSessionStates.put(normalizedUserId, "RUNNING");
        return new PaperSessionStateSnapshot("ps-" + shortCode(normalizedUserId), "RUNNING");
    }

    @Override
    public UserProfileSnapshot getCurrentUserProfile(String userId) {
        String normalizedUserId = normalize(userId, "user-demo-001");
        String code = shortCode(normalizedUserId);
        return new UserProfileSnapshot(
                normalizedUserId,
                "trader_" + code,
                "trader." + code + "@marcus.local",
                "USER"
        );
    }

    @Override
    public UserPreferencesSnapshot updateCurrentUserPreferences(String userId, UserPreferencesUpdateSnapshot request) {
        String normalizedUserId = normalize(userId, "user-demo-001");
        String timezone = normalize(request.timezone(), "UTC");
        String locale = normalize(request.locale(), "en-US");
        boolean emailEnabled = request.emailNotificationsEnabled() == null
                || request.emailNotificationsEnabled();

        return new UserPreferencesSnapshot(
                timezone,
                locale,
                emailEnabled && !normalizedUserId.isBlank()
        );
    }

    @Override
    public List<ApiKeySummarySnapshot> listCurrentUserApiKeys(String userId) {
        String normalizedUserId = normalize(userId, "user-demo-001");
        String code = shortCode(normalizedUserId);
        return List.of(
                new ApiKeySummarySnapshot(
                        "key-" + code + "-1",
                        "Terminal Sync",
                        "mk_live_****" + code.substring(0, 2),
                        timeAt(normalizedUserId + ":key1", -120),
                        timeAt(normalizedUserId + ":key1:last", -2)
                ),
                new ApiKeySummarySnapshot(
                        "key-" + code + "-2",
                        "Research Worker",
                        "mk_live_****" + code.substring(code.length() - 2),
                        timeAt(normalizedUserId + ":key2", -95),
                        timeAt(normalizedUserId + ":key2:last", -10)
                )
        );
    }

    @Override
    public CreateApiKeySnapshot createCurrentUserApiKey(String userId, String label) {
        String normalizedUserId = normalize(userId, "user-demo-001");
        String normalizedLabel = normalize(label, "Default Key");
        String seed = normalizedUserId + ":" + normalizedLabel.toLowerCase(Locale.ROOT);
        String code = shortCode(seed);

        return new CreateApiKeySnapshot(
                "key-" + code,
                "mk_live_" + maskedBlock(seed, 18),
                normalizedLabel
        );
    }

    @Override
    public void deleteCurrentUserApiKey(String userId, String apiKeyId) {
        normalize(userId, "user-demo-001");
        String normalizedApiKeyId = normalize(apiKeyId, "");
        if (!normalizedApiKeyId.startsWith("key-")) {
            throw new NoSuchElementException("API key not found: " + normalizedApiKeyId);
        }
    }

    @Override
    public LoginActivityPageSnapshot listCurrentUserLoginActivities(String userId, int page, int size) {
        String normalizedUserId = normalize(userId, "user-demo-001");
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.max(1, Math.min(size, 100));

        long totalElements = 120L;
        int totalPages = (int) Math.ceil(totalElements / (double) normalizedSize);
        int offset = normalizedPage * normalizedSize;

        List<LoginActivitySnapshot> items = new ArrayList<>(normalizedSize);
        for (int index = 0; index < normalizedSize; index++) {
            int absoluteIndex = offset + index;
            String seed = normalizedUserId + ":login:" + absoluteIndex;
            items.add(new LoginActivitySnapshot(
                    timeAt(seed, -absoluteIndex),
                    "192.168." + boundedInt(seed + ":oct3", 0, 255) + "." + boundedInt(seed + ":oct4", 1, 254),
                    "MarcusTerminal/" + boundedInt(seed + ":ua", 2, 9) + "." + boundedInt(seed + ":ua-minor", 0, 9),
                    boundedInt(seed + ":success", 0, 100) > 8
            ));
        }

        OffsetPaginationMetaSnapshot meta = new OffsetPaginationMetaSnapshot(
                normalizedPage,
                normalizedSize,
                totalElements,
                totalPages,
                normalizedPage + 1 < totalPages
        );
        return new LoginActivityPageSnapshot(items, meta);
    }

    @Override
    public List<SignalItemSnapshot> listSignals(String status, int limit) {
        String normalizedStatus = normalize(status, "ALL").toUpperCase(Locale.ROOT);
        int normalizedLimit = Math.max(1, Math.min(limit, 200));

        List<String> statuses = normalizedStatus.equals("ALL")
                ? List.of("PENDING", "DISPATCHED", "FAILED")
                : List.of(normalizedStatus);

        List<SignalItemSnapshot> items = new ArrayList<>(normalizedLimit);
        for (int index = 0; index < normalizedLimit; index++) {
            String signalId = "sig-" + (index + 1);
            String currentStatus = statuses.get(index % statuses.size());

            items.add(new SignalItemSnapshot(
                    signalId,
                    "bot-" + ((index % 5) + 1),
                    exchangeFromSeed(signalId).toLowerCase(Locale.ROOT),
                    pairFromSeed(signalId),
                    (index % 2 == 0) ? "OPEN_LONG" : "CLOSE_LONG",
                    scaled(signalId + ":price", 45.0, 220.0),
                    currentStatus,
                    timeAt(signalId, -index)
            ));
        }

        return items;
    }

    @Override
    public ConnectivityHealthSnapshot getSystemConnectivityHealth() {
        List<ConnectivityHealthDependencySnapshot> dependencies = List.of(
                new ConnectivityHealthDependencySnapshot("postgres", "UP", 22),
                new ConnectivityHealthDependencySnapshot("redis", "UP", 12),
                new ConnectivityHealthDependencySnapshot("kafka", "DEGRADED", 87)
        );
        return new ConnectivityHealthSnapshot("DEGRADED", BASE_TIME.plusDays(96), dependencies);
    }

    @Override
    public ExecutionLogPageSnapshot listSystemExecutionLogs(String cursor, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        String normalizedCursor = normalize(cursor, "cursor-0");

        List<ExecutionLogItemSnapshot> items = new ArrayList<>(normalizedLimit);
        for (int index = 0; index < normalizedLimit; index++) {
            int seq = boundedInt(normalizedCursor + ":" + index, 1000, 9999);
            items.add(new ExecutionLogItemSnapshot(
                    timeAt(normalizedCursor + ":log:" + index, -index),
                    (index % 8 == 0) ? "WARN" : "INFO",
                    "signal-router",
                    "Execution event #" + seq + " processed"
            ));
        }

        return new ExecutionLogPageSnapshot("cursor-" + maskedBlock(normalizedCursor, 8), items);
    }

    private String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private int decodeCursorOffset(String cursor) {
        String normalizedCursor = normalize(cursor, "paper-cursor-0");
        if (!normalizedCursor.startsWith("paper-cursor-")) {
            return 0;
        }
        String raw = normalizedCursor.substring("paper-cursor-".length());
        try {
            return Math.max(Integer.parseInt(raw), 0);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private String shortCode(String key) {
        int value = Math.abs(Objects.hash(key));
        String raw = Integer.toHexString(value);
        return raw.length() >= 6 ? raw.substring(0, 6) : String.format("%1$6s", raw).replace(' ', '0');
    }

    private String maskedBlock(String key, int length) {
        String code = shortCode(key) + Integer.toHexString(Math.abs(Objects.hash(key, "mask")));
        if (code.length() < length) {
            code = code + "0".repeat(length - code.length());
        }
        return code.substring(0, length);
    }

    private int boundedInt(String key, int minInclusive, int maxInclusive) {
        int span = maxInclusive - minInclusive + 1;
        int offset = Math.floorMod(Objects.hash(key), span);
        return minInclusive + offset;
    }

    private double scaled(String key, double minInclusive, double maxInclusive) {
        int base = Math.floorMod(Objects.hash(key), 10_000);
        double ratio = base / 9_999.0;
        double value = minInclusive + (maxInclusive - minInclusive) * ratio;
        return round4(value);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double round4(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    private LocalDateTime timeAt(String key, int hoursOffset) {
        int dayOffset = boundedInt(key + ":day", 0, 120);
        int minuteOffset = boundedInt(key + ":minute", 0, 59);
        return BASE_TIME.plusDays(dayOffset).plusHours(hoursOffset).plusMinutes(minuteOffset);
    }

    private LocalDateTime seriesTimestamp(int totalPoints, int index, String range) {
        return switch (range) {
            case "1D" -> BASE_TIME.plusDays(96).minusHours(totalPoints - index);
            case "1W" -> BASE_TIME.plusDays(96).minusDays(totalPoints - index);
            case "1M" -> BASE_TIME.plusDays(96).minusDays(totalPoints - index);
            case "YTD" -> BASE_TIME.plusDays(96).minusWeeks(totalPoints - index);
            case "ALL" -> BASE_TIME.plusDays(96).minusMonths(totalPoints - index);
            default -> BASE_TIME.plusDays(96).minusDays(totalPoints - index);
        };
    }

    private String pairFromSeed(String key) {
        String[] pairs = {"BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "ADAUSDT"};
        return pairs[Math.floorMod(Objects.hash(key, "pair"), pairs.length)];
    }

    private String exchangeFromSeed(String key) {
        String[] exchanges = {"BINANCE", "BYBIT", "OKX"};
        return exchanges[Math.floorMod(Objects.hash(key, "exchange"), exchanges.length)];
    }

    private String marketFromSeed(String key) {
        String[] markets = {"CRYPTO", "FOREX", "COMMODITIES"};
        return markets[Math.floorMod(Objects.hash(key, "market"), markets.length)];
    }
}
