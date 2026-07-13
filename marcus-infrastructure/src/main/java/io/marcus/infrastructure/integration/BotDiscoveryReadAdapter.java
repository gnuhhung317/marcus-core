package io.marcus.infrastructure.integration;

import io.marcus.domain.port.BotDiscoveryReadPort;
import io.marcus.domain.port.BotDiscoveryReadPort.BotDetailSnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.BotDiscoveryPageSnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.BotDiscoverySnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.BotPerformanceSnapshot;
import io.marcus.domain.port.BotDiscoveryReadPort.BotSpotlightSnapshot;
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
import io.marcus.domain.service.IdentityService;
import io.marcus.infrastructure.persistence.SpringDataBotDryRunClosedTradeRepository;
import io.marcus.infrastructure.persistence.SpringDataBotHistoricalClosedTradeRepository;
import io.marcus.infrastructure.persistence.SpringDataBotRepository;
import io.marcus.infrastructure.persistence.SpringDataLeaderboardMetricsRepository;
import io.marcus.infrastructure.persistence.SpringDataSignalRepository;
import io.marcus.infrastructure.persistence.SpringDataUserRepository;
import io.marcus.infrastructure.persistence.SpringDataUserSubscriptionRepository;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import io.marcus.infrastructure.persistence.entity.BotHistoricalClosedTradeEntity;
import io.marcus.infrastructure.persistence.entity.BotLeaderboardMetricsEntity;
import io.marcus.infrastructure.persistence.entity.BotLeaderboardMetricsEntity.BotLeaderboardMetricsId;
import io.marcus.infrastructure.persistence.entity.BotDryRunClosedTradeEntity;
import io.marcus.infrastructure.persistence.entity.SignalEntity;
import io.marcus.infrastructure.persistence.entity.UserEntity;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import io.marcus.infrastructure.persistence.executor.ExecutionStateRepository;
import io.marcus.infrastructure.persistence.executor.ExecutionEventRepository;
import io.marcus.infrastructure.persistence.executor.ExecutionStateEntity;
import io.marcus.infrastructure.persistence.executor.ExecutionEventEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;
import io.marcus.infrastructure.cache.RedisCacheFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Real-time adapter for bot discovery and leaderboard operations.
 */
@Component
@Primary
@RequiredArgsConstructor
public class BotDiscoveryReadAdapter implements BotDiscoveryReadPort {

    private static final Duration LEADERBOARD_TTL = Duration.ofSeconds(60);
    private static final Duration MARKETPLACE_TTL = Duration.ofSeconds(120);
    private static final String SOURCE_AUTO = "AUTO";
    private static final String SOURCE_DRY_RUN = "DRY_RUN";
    private static final String SOURCE_HISTORICAL = "HISTORICAL";
    private static final String SOURCE_SIGNAL_BASED = "SIGNAL_BASED";
    private static final TypeReference<BotDetailSnapshot> BOT_DETAIL_TYPE = new TypeReference<>() {};
    private static final TypeReference<BotDiscoveryPageSnapshot> BOT_DISCOVERY_PAGE_TYPE = new TypeReference<>() {};
    private static final TypeReference<LeaderboardBotsPageSnapshot> LEADERBOARD_BOTS_PAGE_TYPE = new TypeReference<>() {};
    private static final TypeReference<LeaderboardFeaturedSnapshot> LEADERBOARD_FEATURED_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<BotSpotlightSnapshot>> BOT_SPOTLIGHTS_TYPE = new TypeReference<>() {};

    private final SpringDataBotRepository springDataBotRepository;
    private final SpringDataSignalRepository springDataSignalRepository;
    private final SpringDataUserSubscriptionRepository springDataUserSubscriptionRepository;
    private final SpringDataUserRepository springDataUserRepository;
    private final SpringDataLeaderboardMetricsRepository leaderboardMetricsRepository;
    private final SpringDataBotDryRunClosedTradeRepository botDryRunClosedTradeRepository;
    private final SpringDataBotHistoricalClosedTradeRepository botHistoricalClosedTradeRepository;
    private final IdentityService identityService;
    private final ExecutionStateRepository executionStateRepository;
    private final ExecutionEventRepository executionEventRepository;
    private final RedisCacheFacade cacheFacade;


    @Override
    @Transactional(readOnly = true)
    public BotDetailSnapshot getBotDetail(String botId, String source) {
        String normalizedBotId = requireBotId(botId);
        String normalizedSource = normalizePerformanceSource(source);
        return cacheFacade.getOrLoad(
                "marketplace:bot-detail:%s:%s".formatted(
                        RedisCacheFacade.keyPart(normalizedBotId),
                        RedisCacheFacade.keyPart(normalizedSource)
                ),
                MARKETPLACE_TTL,
                BOT_DETAIL_TYPE,
                () -> getBotDetailUncached(normalizedBotId, normalizedSource)
        );
    }

    private BotDetailSnapshot getBotDetailUncached(String normalizedBotId, String normalizedSource) {
        BotEntity bot = springDataBotRepository.findByBotIdWithExchange(normalizedBotId)
                .filter(b -> b.getStatus() != BotStatus.DELETED)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found with id: " + normalizedBotId));

        ResolvedPerformance resolvedPerformance = resolvePerformance(bot, normalizedSource);
        SignalMetricsCalculator.MetricsResult metrics = resolvedPerformance.metrics();

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
                resolvedPerformance.performanceSource(),
                bot.getDeveloperId(),
                bot.getApiKey(),
                bot.getCreatedAt(),
                bot.getUpdatedAt(),
                performance);
    }

    private boolean isDiscoverableBot(BotEntity bot) {
        return bot.getStatus() == BotStatus.ACTIVE;
    }

    @Override
    @Transactional(readOnly = true)
    public BotDiscoveryPageSnapshot listPublicBots(String q, String asset, String risk, String sort, int page,
            int size) {
        String key = "marketplace:bots:%s:%s:%s:%s:%d:%d".formatted(
                RedisCacheFacade.keyPart(q),
                RedisCacheFacade.keyPart(asset),
                RedisCacheFacade.keyPart(risk),
                RedisCacheFacade.keyPart(sort),
                page,
                size
        );
        return cacheFacade.getOrLoad(
                key,
                MARKETPLACE_TTL,
                BOT_DISCOVERY_PAGE_TYPE,
                () -> listPublicBotsUncached(q, asset, risk, sort, page, size)
        );
    }

    private BotDiscoveryPageSnapshot listPublicBotsUncached(String q, String asset, String risk, String sort, int page,
            int size) {
        List<BotView> views = springDataBotRepository.findAllWithExchange().stream()
                .filter(this::isDiscoverableBot)
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
    @Transactional(readOnly = true)
    public TradeLogPageSnapshot listBotTrades(String botId, int page, int size, String asset) {
        String normalizedBotId = requireBotId(botId);
        springDataBotRepository.findByBotId(normalizedBotId)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found with id: " + normalizedBotId));

        String normalizedAsset = normalizeAsset(asset);

        Optional<String> currentUserIdOpt = identityService.getCurrentUserId();
        if (currentUserIdOpt.isEmpty()) {
            return new TradeLogPageSnapshot(List.of(), page, size, 0);
        }

        String userId = currentUserIdOpt.get();
        boolean hasActiveSubscription = springDataUserSubscriptionRepository
                .findByUserIdAndBotIdAndStatus(userId, normalizedBotId, SubscriptionStatus.ACTIVE)
                .isPresent();
        if (!hasActiveSubscription) {
            return new TradeLogPageSnapshot(List.of(), page, size, 0);
        }

        // Fetch execution states and signals
        List<Object[]> rows = executionStateRepository.findClosedExecutionStatesAndSignalsForBot(normalizedBotId, normalizedAsset);

        // Sort by closedAt descending
        rows.sort((a, b) -> {
            ExecutionStateEntity esA = (ExecutionStateEntity) a[0];
            ExecutionStateEntity esB = (ExecutionStateEntity) b[0];
            Instant tA = esA.getClosedAt() != null ? esA.getClosedAt() : Instant.MIN;
            Instant tB = esB.getClosedAt() != null ? esB.getClosedAt() : Instant.MIN;
            return tB.compareTo(tA);
        });

        int totalElements = rows.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<Object[]> pagedRows = rows.subList(fromIndex, toIndex);

        List<TradeLogSnapshot> pagedItems = new ArrayList<>();
        for (Object[] row : pagedRows) {
            ExecutionStateEntity es = (ExecutionStateEntity) row[0];
            SignalEntity s = (SignalEntity) row[1];
            pagedItems.add(mapToTradeLogSnapshot(es, s));
        }

        return new TradeLogPageSnapshot(pagedItems, page, size, totalElements);
    }

    @Override
    @Transactional(readOnly = true)
    public TradeLogPageSnapshot listUserTrades(String userId, int page, int size, String asset) {
        if (userId == null || userId.isBlank()) {
            return new TradeLogPageSnapshot(List.of(), page, size, 0);
        }

        // Retrieve all user subscriptions
        List<UserSubscriptionEntity> subscriptions = springDataUserSubscriptionRepository
                .findByUserIdAndStatus(userId, null);
        if (subscriptions.isEmpty()) {
            return new TradeLogPageSnapshot(List.of(), page, size, 0);
        }

        List<String> botIds = subscriptions.stream()
                .map(UserSubscriptionEntity::getBotId)
                .filter(id -> id != null)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        if (botIds.isEmpty()) {
            return new TradeLogPageSnapshot(List.of(), page, size, 0);
        }

        String normalizedAsset = normalizeAsset(asset);

        // Fetch execution states and signals for user's subscribed bots
        List<Object[]> rows = executionStateRepository.findClosedExecutionStatesAndSignalsForBots(botIds, normalizedAsset);

        // Sort by closedAt descending
        rows.sort((a, b) -> {
            ExecutionStateEntity esA = (ExecutionStateEntity) a[0];
            ExecutionStateEntity esB = (ExecutionStateEntity) b[0];
            Instant tA = esA.getClosedAt() != null ? esA.getClosedAt() : Instant.MIN;
            Instant tB = esB.getClosedAt() != null ? esB.getClosedAt() : Instant.MIN;
            return tB.compareTo(tA);
        });

        int totalElements = rows.size();
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<Object[]> pagedRows = rows.subList(fromIndex, toIndex);

        List<TradeLogSnapshot> pagedItems = new ArrayList<>();
        for (Object[] row : pagedRows) {
            ExecutionStateEntity es = (ExecutionStateEntity) row[0];
            SignalEntity s = (SignalEntity) row[1];
            pagedItems.add(mapToTradeLogSnapshot(es, s));
        }

        return new TradeLogPageSnapshot(pagedItems, page, size, totalElements);
    }

    private TradeLogSnapshot mapToTradeLogSnapshot(ExecutionStateEntity es, SignalEntity s) {
        List<ExecutionEventEntity> events = executionEventRepository.findBySignalIdOrderBySequenceAsc(es.getSignalId());

        double entryPrice = 0.0;
        double exitPrice = 0.0;
        double sizeVal = s.getAmount() != null ? s.getAmount().doubleValue() : 0.0;
        double netPnl = 0.0;

        for (ExecutionEventEntity event : events) {
            JsonNode payload = event.getPayload();
            String type = normalizeExecutionEventType(event.getEventType());

            if ("ORDER_FILLED".equals(type)) {
                double fillPrice = 0.0;
                if (payload.has("fill_price")) {
                    fillPrice = payload.get("fill_price").asDouble();
                } else if (payload.has("price")) {
                    fillPrice = payload.get("price").asDouble();
                }

                if (entryPrice == 0.0) {
                    entryPrice = fillPrice;
                } else {
                    exitPrice = fillPrice;
                }
            } else if ("POSITION_OPENED".equals(type)) {
                if (payload.has("position_size")) {
                    sizeVal = payload.get("position_size").asDouble();
                } else if (payload.has("size")) {
                    sizeVal = payload.get("size").asDouble();
                }
            } else if ("POSITION_CLOSED".equals(type)) {
                if (payload.has("pnl")) {
                    netPnl = payload.get("pnl").asDouble();
                } else if (payload.has("netPnl")) {
                    netPnl = payload.get("netPnl").asDouble();
                } else if (payload.has("realized_pnl")) {
                    netPnl = payload.get("realized_pnl").asDouble();
                }

                if (payload.has("exit_price")) {
                    exitPrice = payload.get("exit_price").asDouble();
                } else if (payload.has("price") && exitPrice == 0.0) {
                    exitPrice = payload.get("price").asDouble();
                }
            }
        }

        if (entryPrice == 0.0 && s.getEntry() != null) {
            entryPrice = s.getEntry().doubleValue();
        }

        if (exitPrice == 0.0 && entryPrice != 0.0 && sizeVal != 0.0) {
            boolean isLong = s.getAction() == io.marcus.domain.vo.SignalAction.OPEN_LONG;
            if (isLong) {
                exitPrice = entryPrice + (netPnl / sizeVal);
            } else {
                exitPrice = entryPrice - (netPnl / sizeVal);
            }
        }

        LocalDateTime timestamp = es.getClosedAt() != null
                ? LocalDateTime.ofInstant(es.getClosedAt(), ZoneOffset.UTC)
                : LocalDateTime.now();

        String sideStr = (s.getAction() == io.marcus.domain.vo.SignalAction.OPEN_SHORT) ? "SHORT" : "LONG";

        return new TradeLogSnapshot(
                timestamp,
                s.getSymbol(),
                sideStr,
                sizeVal,
                entryPrice,
                exitPrice,
                netPnl
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LeaderboardBotsPageSnapshot listLeaderboardBots(
            LeaderboardDataSource dataSource, String market, String asset, LeaderboardRankMetric rankMetric, int page,
            int size) {
        String key = "leaderboard:bots:%s:%s:%s:%s:%d:%d".formatted(
                dataSource.name(),
                RedisCacheFacade.keyPart(market),
                RedisCacheFacade.keyPart(asset),
                rankMetric.name(),
                page,
                size
        );
        return cacheFacade.getOrLoad(
                key,
                LEADERBOARD_TTL,
                LEADERBOARD_BOTS_PAGE_TYPE,
                () -> listLeaderboardBotsUncached(dataSource, market, asset, rankMetric, page, size)
        );
    }

    private String normalizeExecutionEventType(String eventType) {
        if (eventType == null) {
            return "";
        }
        return eventType.trim()
                .replace('.', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);
    }

    private LeaderboardBotsPageSnapshot listLeaderboardBotsUncached(
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
        return cacheFacade.getOrLoad(
                "leaderboard:featured",
                LEADERBOARD_TTL,
                LEADERBOARD_FEATURED_TYPE,
                this::listLeaderboardFeaturedUncached
        );
    }

    private LeaderboardFeaturedSnapshot listLeaderboardFeaturedUncached() {
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
        return cacheFacade.getOrLoad(
                "leaderboard:spotlights",
                LEADERBOARD_TTL,
                BOT_SPOTLIGHTS_TYPE,
                this::listLeaderboardSpotlightsUncached
        );
    }

    private List<BotSpotlightSnapshot> listLeaderboardSpotlightsUncached() {
        LeaderboardBotsPageSnapshot bots = listLeaderboardBots(LeaderboardDataSource.DRY_RUN, null, null,
                LeaderboardRankMetric.CAGR, 0, 5);
        return bots.items().stream()
                .map(s -> new BotSpotlightSnapshot(s.botId(), s.botName(), "CRYPTO", s.cagr()))
                .toList();
    }

    private BotView toBotView(BotEntity bot) {
        ResolvedPerformance resolvedPerformance = resolvePerformance(bot, SOURCE_AUTO);
        long subscribers = springDataUserSubscriptionRepository
                .findByBotIdAndStatusOrderByCreatedAtDesc(bot.getBotId(), SubscriptionStatus.ACTIVE).size();
        return new BotView(
                bot,
                resolvedPerformance.metrics(),
                subscribers,
                resolvedPerformance.hasPerformanceData(),
                resolvedPerformance.performanceSource()
        );
    }

    private ResolvedPerformance resolvePerformance(BotEntity bot, String requestedSource) {
        String normalizedSource = normalizePerformanceSource(requestedSource);

        if (SOURCE_DRY_RUN.equals(normalizedSource)) {
            ResolvedPerformance dryRun = resolveLeaderboardPerformance(bot, SOURCE_DRY_RUN);
            return dryRun.hasPerformanceData() ? dryRun : resolvePerformance(bot, SOURCE_AUTO);
        }

        if (SOURCE_HISTORICAL.equals(normalizedSource)) {
            ResolvedPerformance historical = resolveLeaderboardPerformance(bot, SOURCE_HISTORICAL);
            return historical.hasPerformanceData() ? historical : resolvePerformance(bot, SOURCE_AUTO);
        }

        ResolvedPerformance dryRun = resolveLeaderboardPerformance(bot, SOURCE_DRY_RUN);
        if (dryRun.hasPerformanceData()) {
            return dryRun;
        }

        ResolvedPerformance historical = resolveLeaderboardPerformance(bot, SOURCE_HISTORICAL);
        if (historical.hasPerformanceData()) {
            return historical;
        }

        return resolveSignalPerformance(bot);
    }

    private ResolvedPerformance resolveLeaderboardPerformance(BotEntity bot, String source) {
        String botId = bot.getBotId();
        Optional<BotLeaderboardMetricsEntity> leaderboardMetrics = leaderboardMetricsRepository
                .findById(new BotLeaderboardMetricsId(botId, source));
        if (leaderboardMetrics.isPresent()) {
            return new ResolvedPerformance(
                    toMarketplaceMetrics(leaderboardMetrics.get(), tradeStatsFor(botId, source)),
                    true,
                    source
            );
        }

        return new ResolvedPerformance(calculateMetrics(List.of()), false, null);
    }

    private ResolvedPerformance resolveSignalPerformance(BotEntity bot) {
        String botId = bot.getBotId();
        List<SignalEntity> signals = springDataSignalRepository
                .findByBotIdAndGeneratedTimestampIsNotNullOrderByGeneratedTimestampAsc(botId);
        if (signals.isEmpty()) {
            return new ResolvedPerformance(calculateMetrics(List.of()), false, null);
        }

        return new ResolvedPerformance(calculateMetrics(signals), true, SOURCE_SIGNAL_BASED);
    }

    private SignalMetricsCalculator.MetricsResult toMarketplaceMetrics(BotLeaderboardMetricsEntity metrics, TradeStats tradeStats) {
        double annualReturn = metrics.getCagr();
        double maxDrawdown = Math.abs(metrics.getMaxDrawdown());
        double sharpe = metrics.getSharpe();
        double winRate = tradeStats.winRate();
        double avgTradeReturn = annualReturn;
        double tradesPerDay = tradeStats.totalTrades();
        String risk = SignalMetricsCalculator.classifyRisk(annualReturn, maxDrawdown);
        return new SignalMetricsCalculator.MetricsResult(
                annualReturn,
                maxDrawdown,
                sharpe,
                winRate,
                avgTradeReturn,
                tradesPerDay,
                risk
        );
    }

    private TradeStats tradeStatsFor(String botId, String dataSource) {
        if (LeaderboardDataSource.DRY_RUN.name().equalsIgnoreCase(dataSource)) {
            return tradeStatsFromDryRun(botDryRunClosedTradeRepository.findByBotIdOrderByExitTimestampAsc(botId));
        }
        return tradeStatsFromHistorical(botHistoricalClosedTradeRepository.findByBotIdOrderByExitTimestampAsc(botId));
    }

    private TradeStats tradeStatsFromDryRun(List<BotDryRunClosedTradeEntity> trades) {
        if (trades == null || trades.isEmpty()) {
            return TradeStats.empty();
        }
        long winningTrades = trades.stream()
                .map(BotDryRunClosedTradeEntity::getPnl)
                .mapToDouble(pnl -> pnl == null ? 0.0d : pnl.doubleValue())
                .filter(pnl -> pnl > 0.0d)
                .count();
        return new TradeStats(trades.size(), winningTrades);
    }

    private TradeStats tradeStatsFromHistorical(List<BotHistoricalClosedTradeEntity> trades) {
        if (trades == null || trades.isEmpty()) {
            return TradeStats.empty();
        }
        long winningTrades = trades.stream()
                .map(BotHistoricalClosedTradeEntity::getPnl)
                .mapToDouble(pnl -> pnl == null ? 0.0d : pnl.doubleValue())
                .filter(pnl -> pnl > 0.0d)
                .count();
        return new TradeStats(trades.size(), winningTrades);
    }

    private BotDiscoverySnapshot toDiscoverySnapshot(BotView view) {
        BotEntity bot = view.bot();
        SignalMetricsCalculator.MetricsResult metrics = view.metrics();
        if (!view.hasPerformanceData()) {
            return new BotDiscoverySnapshot(
                    bot.getBotId(),
                    bot.getName(),
                    bot.getDescription(),
                    bot.getTradingPair(),
                    metrics.risk(),
                    null,
                    null,
                    null,
                    view.performanceSource(),
                    (int) view.subscribers());
        }
        return new BotDiscoverySnapshot(
                bot.getBotId(),
                bot.getName(),
                bot.getDescription(),
                bot.getTradingPair(),
                metrics.risk(),
                metrics.annualReturn(),
                metrics.maxDrawdown(),
                metrics.winRate(),
                view.performanceSource(),
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
            case "return" -> Comparator.comparingDouble((BotDiscoverySnapshot snapshot) -> safeDouble(snapshot.annualReturn()))
                    .thenComparing(BotDiscoverySnapshot::botName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "drawdown" -> Comparator
                    .comparingDouble((BotDiscoverySnapshot snapshot) -> Math.abs(safeDouble(snapshot.maxDrawdown())))
                    .thenComparing(BotDiscoverySnapshot::botName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "-drawdown" -> Comparator.comparingDouble((BotDiscoverySnapshot snapshot) -> safeDouble(snapshot.maxDrawdown())).reversed()
                    .thenComparing(BotDiscoverySnapshot::botName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "subscribers" -> Comparator.comparingInt(BotDiscoverySnapshot::subscribers)
                    .thenComparing(BotDiscoverySnapshot::botName, Comparator.nullsLast(String::compareToIgnoreCase));
            case "-subscribers" -> Comparator.comparingInt(BotDiscoverySnapshot::subscribers).reversed()
                    .thenComparing(BotDiscoverySnapshot::botName, Comparator.nullsLast(String::compareToIgnoreCase));
            default -> Comparator.comparingDouble((BotDiscoverySnapshot snapshot) -> safeDouble(snapshot.annualReturn())).reversed()
                    .thenComparing(BotDiscoverySnapshot::botName, Comparator.nullsLast(String::compareToIgnoreCase));
        };
    }

    private double safeDouble(Double value) {
        return value == null ? 0.0d : value;
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

    private String normalizePerformanceSource(String source) {
        if (source == null || source.isBlank()) {
            return SOURCE_AUTO;
        }

        String normalizedSource = source.trim().toUpperCase(Locale.ROOT);
        if (SOURCE_DRY_RUN.equals(normalizedSource) || SOURCE_HISTORICAL.equals(normalizedSource)) {
            return normalizedSource;
        }

        return SOURCE_AUTO;
    }

    private String requireUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        return userId.trim();
    }

    private record BotView(
            BotEntity bot,
            SignalMetricsCalculator.MetricsResult metrics,
            long subscribers,
            boolean hasPerformanceData,
            String performanceSource) {
    }

    private record ResolvedPerformance(
            SignalMetricsCalculator.MetricsResult metrics,
            boolean hasPerformanceData,
            String performanceSource) {
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

    private record TradeStats(long totalTrades, long winningTrades) {

        static TradeStats empty() {
            return new TradeStats(0, 0);
        }

        double winRate() {
            return totalTrades == 0 ? 0.0d : winningTrades / (double) totalTrades;
        }
    }
}
