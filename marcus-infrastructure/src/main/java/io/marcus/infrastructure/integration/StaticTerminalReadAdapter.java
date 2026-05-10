package io.marcus.infrastructure.integration;

import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.service.SignalMetricsCalculator;
import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.infrastructure.persistence.SpringDataBotRepository;
import io.marcus.infrastructure.persistence.SpringDataSignalRepository;
import io.marcus.infrastructure.persistence.SpringDataUserSubscriptionRepository;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import io.marcus.infrastructure.persistence.entity.SignalEntity;
import io.marcus.infrastructure.persistence.entity.UserPortfolioEntity;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final io.marcus.infrastructure.persistence.SpringDataUserPortfolioRepository springDataUserPortfolioRepository;

    @Override
    @Transactional(readOnly = true)
    public BotDetailSnapshot getBotDetail(String botId) {
        String normalizedBotId = requireNonBlank(botId, "botId");
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
                .countByStatusGroupByBotId(SubscriptionStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1],
                        (a, b) -> a
                ));

        Map<String, List<SignalEntity>> signalsByBotId = springDataBotRepository.findAllWithExchange()
                .stream()
                .collect(Collectors.toMap(
                        BotEntity::getBotId,
                        bot -> springDataSignalRepository.findByBotId(bot.getBotId()),
                        (a, b) -> a
                ));

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
        // Not yet implemented — return toggled state without persistence
        return new FavoriteStrategySnapshot(strategyId != null ? strategyId : "", true);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewSnapshot getDashboardOverview(String userId) {
        String normalizedUserId = requireNonBlank(userId, "userId");
        UserPortfolioEntity portfolio = findPortfolioOrDefault(normalizedUserId);

        List<UserSubscriptionEntity> activeSubscriptions = springDataUserSubscriptionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(normalizedUserId, SubscriptionStatus.ACTIVE);

        List<String> botIds = activeSubscriptions.stream()
                .map(UserSubscriptionEntity::getBotId)
                .toList();

        List<SignalEntity> relatedSignals = botIds.isEmpty()
                ? List.of()
                : springDataSignalRepository.findByBotIdInAndGeneratedTimestampIsNotNullOrderByGeneratedTimestampAsc(botIds);

        List<SignalMetricsCalculator.SignalData> signalDataList = relatedSignals.stream()
                .map(this::toSignalData)
                .toList();

        long successfulSignals = signalDataList.stream()
                .filter(s -> SignalMetricsCalculator.deriveReturn(s) > 0)
                .count();

        double totalEquity = SignalMetricsCalculator.round2(portfolio.getTotalCapital().doubleValue());
        double openPnl = SignalMetricsCalculator.round2(portfolio.getUnrealizedPnl().doubleValue());
        double winRate = signalDataList.isEmpty() ? 0.0
                : SignalMetricsCalculator.round4(successfulSignals / (double) signalDataList.size());
        int activeBots = activeSubscriptions.size();
        return new DashboardOverviewSnapshot(totalEquity, openPnl, winRate, activeBots);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSeriesPointSnapshot> listDashboardEquitySeries(String userId, String range) {
        String normalizedUserId = requireNonBlank(userId, "userId");
        UserPortfolioEntity portfolio = findPortfolioOrDefault(normalizedUserId);
        String normalizedRange = normalize(range, "1M").toUpperCase(Locale.ROOT);
        int points = pointsForRange(normalizedRange);

        List<String> subscribedBotIds = springDataUserSubscriptionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(normalizedUserId, SubscriptionStatus.ACTIVE)
                .stream()
                .map(UserSubscriptionEntity::getBotId)
                .toList();

        if (subscribedBotIds.isEmpty()) {
            return List.of();
        }

        List<SignalEntity> orderedSignals = springDataSignalRepository
                .findByBotIdInAndGeneratedTimestampIsNotNullOrderByGeneratedTimestampAsc(subscribedBotIds);

        if (orderedSignals.isEmpty()) {
            return List.of();
        }

        int startIndex = Math.max(0, orderedSignals.size() - points);
        List<SignalEntity> window = orderedSignals.subList(startIndex, orderedSignals.size());
        
        // Calculate the net sum of return points within this window to find base anchor
        double totalWindowImpact = window.stream()
                .mapToDouble(s -> SignalMetricsCalculator.deriveReturn(toSignalData(s)) * 1_000.0)
                .sum();
        
        double currentEquity = portfolio.getTotalCapital().doubleValue();
        double historicalAnchor = currentEquity - totalWindowImpact;
        
        List<TimeSeriesPointSnapshot> result = new ArrayList<>(window.size());
        double rollingTotal = historicalAnchor;
        
        for (SignalEntity signal : window) {
            double stepImpact = SignalMetricsCalculator.deriveReturn(toSignalData(signal)) * 1_000.0;
            rollingTotal += stepImpact;
            result.add(new TimeSeriesPointSnapshot(signal.getGeneratedTimestamp(),
                    SignalMetricsCalculator.round4(rollingTotal)));
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeAllocationSnapshot> listExchangeAllocation(String userId) {
        String normalizedUserId = requireNonBlank(userId, "userId");
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
                .map(entry -> new ExchangeAllocationSnapshot(entry.getKey(),
                        SignalMetricsCalculator.round2(entry.getValue() / (double) total)))
                .sorted((left, right) -> Double.compare(right.percentage(), left.percentage()))
                .toList();
    }

    // --- Not yet implemented: Strategy, Leaderboard, Paper Trading, User Settings ---
    // These return empty/default responses until real backend data is available.

    @Override
    public StrategyDetailSnapshot getStrategyDetail(String strategyId) {
        return new StrategyDetailSnapshot(strategyId, "", "", "", "INACTIVE");
    }

    @Override
    public StrategyMetricsSnapshot getStrategyMetrics(String strategyId, String feeMode) {
        return new StrategyMetricsSnapshot(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    }

    @Override
    public List<TimeSeriesPointSnapshot> listStrategyPerformanceSeries(String strategyId, String range) {
        return List.of();
    }

    @Override
    public TradeLogPageSnapshot listStrategyTrades(String strategyId, int page, int size, String asset) {
        return new TradeLogPageSnapshot(List.of(), Math.max(page, 0), Math.max(1, size), 0L);
    }

    @Override
    public LeaderboardStrategiesPageSnapshot listLeaderboardStrategies(
            String timeframe, String market, String asset, String rankMetric, int page, int size
    ) {
        int p = Math.max(page, 0);
        int s = Math.max(1, Math.min(size, 100));
        return new LeaderboardStrategiesPageSnapshot(List.of(),
                new OffsetPaginationMetaSnapshot(p, s, 0, 0, false));
    }

    @Override
    public LeaderboardFeaturedSnapshot listLeaderboardFeatured() {
        return new LeaderboardFeaturedSnapshot(List.of());
    }

    @Override
    public List<StrategySpotlightSnapshot> listLeaderboardSpotlights() {
        return List.of();
    }

    @Override
    public PaperSessionSummarySnapshot getPaperSessionSummary(String userId) {
        return new PaperSessionSummarySnapshot("", "STOPPED", 0.0, 0.0, 0.0);
    }

    @Override
    public List<PaperSignalSnapshot> listPaperSignals(String status, int limit) {
        return List.of();
    }

    @Override
    public PaperExecutionLogPageSnapshot listPaperExecutionLogs(String userId, String cursor, int limit) {
        return new PaperExecutionLogPageSnapshot(List.of(),
                new CursorPaginationMetaSnapshot(cursor, null, Math.max(1, limit), false));
    }

    @Override
    public PaperOrderSnapshot createPaperOrder(String userId, PaperOrderCreateSnapshot request) {
        return new PaperOrderSnapshot("", "REJECTED", 0.0);
    }

    @Override
    public PaperSessionStateSnapshot pausePaperSession(String userId) {
        return new PaperSessionStateSnapshot("", "PAUSED");
    }

    @Override
    public PaperSessionStateSnapshot resumePaperSession(String userId) {
        return new PaperSessionStateSnapshot("", "RUNNING");
    }

    @Override
    public UserProfileSnapshot getCurrentUserProfile(String userId) {
        return new UserProfileSnapshot(userId != null ? userId : "", "", "", "USER");
    }

    @Override
    public UserPreferencesSnapshot updateCurrentUserPreferences(String userId, UserPreferencesUpdateSnapshot request) {
        return new UserPreferencesSnapshot(
                request != null && request.timezone() != null ? request.timezone() : "UTC",
                request != null && request.locale() != null ? request.locale() : "en-US",
                request != null && request.emailNotificationsEnabled() != null
                        ? request.emailNotificationsEnabled() : true
        );
    }

    @Override
    public List<ApiKeySummarySnapshot> listCurrentUserApiKeys(String userId) {
        return List.of();
    }

    @Override
    public CreateApiKeySnapshot createCurrentUserApiKey(String userId, String label) {
        return new CreateApiKeySnapshot("", "", label != null ? label : "");
    }

    @Override
    public void deleteCurrentUserApiKey(String userId, String apiKeyId) {
        // No-op until real API key management is implemented
    }

    @Override
    public LoginActivityPageSnapshot listCurrentUserLoginActivities(String userId, int page, int size) {
        int p = Math.max(page, 0);
        int s = Math.max(1, Math.min(size, 100));
        return new LoginActivityPageSnapshot(List.of(),
                new OffsetPaginationMetaSnapshot(p, s, 0, 0, false));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SignalItemSnapshot> listSignals(String status, int limit) {
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        String normalizedStatus = (status == null || status.isBlank()) ? "ALL" : status.trim().toUpperCase(Locale.ROOT);

        List<SignalEntity> signals;
        if ("ALL".equals(normalizedStatus)) {
            signals = springDataSignalRepository.findAllOrderByGeneratedTimestampDesc(
                    PageRequest.of(0, normalizedLimit));
        } else {
            signals = springDataSignalRepository.findByStatusStringOrderByGeneratedTimestampDesc(
                    normalizedStatus, PageRequest.of(0, normalizedLimit));
        }

        return signals.stream()
                .map(signal -> new SignalItemSnapshot(
                        signal.getSignalId(),
                        signal.getBotId(),
                        resolveExchangeForSignal(signal.getBotId()),
                        signal.getSymbol(),
                        signal.getAction() != null ? signal.getAction().name() : "",
                        signal.getEntry() != null ? signal.getEntry().doubleValue() : 0.0,
                        signal.getStatus() != null ? signal.getStatus().name() : "",
                        signal.getGeneratedTimestamp()
                ))
                .toList();
    }

    @Override
    public ConnectivityHealthSnapshot getSystemConnectivityHealth() {
        // Return real-time health when monitoring is implemented
        return new ConnectivityHealthSnapshot("UNKNOWN", LocalDateTime.now(), List.of());
    }

    @Override
    public ExecutionLogPageSnapshot listSystemExecutionLogs(String cursor, int limit) {
        return new ExecutionLogPageSnapshot(cursor, List.of());
    }

    private BotDiscoverySnapshot toDiscoverySnapshot(BotEntity bot, long subscribers, BotMetrics metrics) {
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
    }

    private Comparator<BotDiscoverySnapshot> comparatorForDiscoverySnapshot(String sort) {
        return switch (sort) {
            case "return" ->
                Comparator.comparingDouble(BotDiscoverySnapshot::annualReturn);
            case "-return" ->
                Comparator.comparingDouble(BotDiscoverySnapshot::annualReturn).reversed();
            case "drawdown" ->
                Comparator.comparingDouble(BotDiscoverySnapshot::maxDrawdown);
            case "-drawdown" ->
                Comparator.comparingDouble(BotDiscoverySnapshot::maxDrawdown).reversed();
            case "subscribers" ->
                Comparator.comparingInt(BotDiscoverySnapshot::subscribers);
            case "-subscribers" ->
                Comparator.comparingInt(BotDiscoverySnapshot::subscribers).reversed();
            default ->
                throw new IllegalArgumentException("Unsupported sort: " + sort);
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
        List<SignalEntity> signals = springDataSignalRepository.findByBotId(botId);
        long subscribers = springDataUserSubscriptionRepository
                .countByStatusGroupByBotId(SubscriptionStatus.ACTIVE)
                .stream()
                .filter(row -> botId.equals(row[0]))
                .map(row -> (Long) row[1])
                .findFirst()
                .orElse(0L);
        return calculateBotMetrics(botId, signals, subscribers);
    }

    private BotMetrics calculateBotMetrics(String botId, List<SignalEntity> signals, long subscribers) {
        if (signals == null || signals.isEmpty()) {
            return new BotMetrics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    SignalMetricsCalculator.classifyRisk(0.0, 0.0), subscribers);
        }

        List<SignalMetricsCalculator.SignalData> signalDataList = signals.stream()
                .map(this::toSignalData)
                .toList();

        SignalMetricsCalculator.MetricsResult result = SignalMetricsCalculator.calculate(
                signalDataList, calculateAgeDays(signals));

        return new BotMetrics(result.annualReturn(), result.maxDrawdown(), result.sharpe(),
                result.winRate(), result.avgTradeReturn(), result.tradesPerDay(),
                result.risk(), subscribers);
    }

    private long calculateAgeDays(List<SignalEntity> signals) {
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

    /** Convert JPA entity to domain-layer signal data for calculation. */
    private SignalMetricsCalculator.SignalData toSignalData(SignalEntity signal) {
        return new SignalMetricsCalculator.SignalData(
                signal.getEntry(), signal.getTakeProfit(),
                signal.getStopLoss(), signal.getAction()
        );
    }

    /** Resolve exchange slug for a signal's bot. Falls back to "unknown". */
    private String resolveExchangeForSignal(String botId) {
        return springDataBotRepository.findByBotIdWithExchange(botId)
                .map(this::resolveExchangeLabel)
                .orElse("unknown")
                .toLowerCase(Locale.ROOT);
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

    private record BotMetrics(
            double annualReturn, double maxDrawdown, double sharpe,
            double winRate, double avgTradeReturn, double tradesPerDay,
            String risk, long subscribers
    ) {}

    private String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? fallback : trimmed;
    }

    private String requireNonBlank(String value, String paramName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(paramName + " must not be blank");
        }
        return value.trim();
    }

    // Pha 1: Decision Dashboard - Portfolio-level queries
    @Override
    @Transactional(readOnly = true)
    public PortfolioOverviewSnapshot getPortfolioOverview(String userId) {
        List<UserSubscriptionEntity> activeSubscriptions = springDataUserSubscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);

        UserPortfolioEntity portfolio = findPortfolioOrDefault(userId);
        int activeBotsCount = activeSubscriptions.size();
        double totalCapital = portfolio.getTotalCapital().doubleValue();

        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        List<SignalEntity> allSignals24h = activeSubscriptions.stream()
                .flatMap(sub -> springDataSignalRepository.findByBotIdAndCreatedAtAfter(sub.getBotId(), yesterday).stream())
                .toList();

        long successfulSignals = allSignals24h.stream()
                .filter(signal -> SignalMetricsCalculator.deriveReturn(toSignalData(signal)) > 0)
                .count();
        double aggregateWinRate24h = allSignals24h.isEmpty() ? 0.0
                : SignalMetricsCalculator.round4((double) successfulSignals / allSignals24h.size());

        int atRiskCount = (int) activeSubscriptions.stream()
                .filter(sub -> calculateDrawdown(sub) < -Math.abs(portfolio.getMaxDrawdownThreshold().doubleValue()))
                .count();

        double aggregateOpenPnL = activeSubscriptions.stream()
                .mapToDouble(sub -> calculateCurrentPnL(sub, portfolio))
                .sum();

        return new PortfolioOverviewSnapshot(
                activeBotsCount,
                totalCapital,
                aggregateWinRate24h,
                atRiskCount,
                totalCapital,
                aggregateOpenPnL,
                LocalDateTime.now()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<SubscriptionDecisionSnapshot> getSubscriptionDecisions(String userId, String statusFilter) {
        SubscriptionStatus status = parseStatusFilter(statusFilter);
        List<UserSubscriptionEntity> subscriptions = springDataUserSubscriptionRepository
                .findByUserIdAndStatus(userId, status);

        return subscriptions.stream()
                .map(this::enrichSubscriptionWithDecisionReason)
                .sorted(
                        Comparator.comparingInt((SubscriptionDecisionSnapshot snap) -> reasonPriority(snap.reason()))
                                .thenComparing(SubscriptionDecisionSnapshot::riskScore, Comparator.reverseOrder())
                )
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionDecisionSnapshot getSubscriptionDecision(String subscriptionId) {
        UserSubscriptionEntity subscription = springDataUserSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new NoSuchElementException("Subscription not found: " + subscriptionId));
        return enrichSubscriptionWithDecisionReason(subscription);
    }

    // Helper: Enrich subscription with decision reason tag
    private SubscriptionDecisionSnapshot enrichSubscriptionWithDecisionReason(UserSubscriptionEntity subscription) {
        UserPortfolioEntity portfolio = findPortfolioOrDefault(subscription.getUserId());
        
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        List<SignalEntity> signals24h = springDataSignalRepository.findByBotIdAndCreatedAtAfter(
                subscription.getBotId(),
                yesterday
        );

        long successfulSignals = signals24h.stream()
                .filter(signal -> SignalMetricsCalculator.deriveReturn(toSignalData(signal)) > 0)
                .count();
        double winRate = signals24h.isEmpty() ? 0.0
                : SignalMetricsCalculator.round4((double) successfulSignals / signals24h.size());
        double drawdown = calculateDrawdown(subscription);
        double currentPnL = calculateCurrentPnL(subscription, portfolio);
        double failureRate = signals24h.isEmpty() ? 0.0 : 1.0 - winRate;

        // Determine decision reason
        TerminalReadPort.DecisionReason reason = determineReason(winRate, drawdown, signals24h, failureRate, portfolio);
        String explanation = generateReasonExplanation(reason, winRate, drawdown);
        double riskScore = Math.max(0.0, Math.min(1.0, 1.0 + drawdown));  // 0=safe, 1=high risk

        BotEntity bot = springDataBotRepository.findByBotId(subscription.getBotId())
                .orElseThrow(() -> new NoSuchElementException("Bot not found: " + subscription.getBotId()));

        return new SubscriptionDecisionSnapshot(
                subscription.getId(),
                subscription.getBotId(),
                bot.getName(),
                "/api/icons/bot/" + subscription.getBotId() + ".png", // Default icon URL
                subscription.getStatus().name(),
                currentPnL,
                currentPnL / portfolio.getTotalCapital().doubleValue(), // Dynamic total portfolio denominator
                drawdown,
                winRate,
                signals24h.size(),
                (int) successfulSignals,
                reason,
                explanation,
                riskScore,
                daysSinceSubscribed(subscription),
                daysInNegative(subscription),
                signals24h.isEmpty() ? null : signals24h.get(0).getGeneratedTimestamp(),
                resolveExchangeLabel(bot)
        );
    }

    // Helper: Determine decision reason from metrics
    private TerminalReadPort.DecisionReason determineReason(
            double winRate,
            double drawdown,
            List<SignalEntity> signals,
            double failureRate,
            UserPortfolioEntity portfolio
    ) {
        double highRiskThresh = -Math.abs(portfolio.getMaxDrawdownThreshold().doubleValue());
        double medRiskThresh = -Math.abs(portfolio.getMediumRiskThreshold().doubleValue());

        if (drawdown < highRiskThresh) {
            return TerminalReadPort.DecisionReason.HIGH_RISK;
        }
        if (drawdown < medRiskThresh || failureRate > 0.20) {
            return TerminalReadPort.DecisionReason.NEEDS_REVIEW;
        }
        LocalDateTime fourHoursAgo = LocalDateTime.now().minusHours(4);
        boolean hasRecentSignals = !signals.isEmpty() && signals.stream()
                .anyMatch(s -> s.getGeneratedTimestamp() != null && s.getGeneratedTimestamp().isAfter(fourHoursAgo));
        if (signals.isEmpty() || !hasRecentSignals) {
            return TerminalReadPort.DecisionReason.SLIPPING;
        }
        if (winRate > 0.60) {
            return TerminalReadPort.DecisionReason.SOLID_PERFORMER;
        }
        return TerminalReadPort.DecisionReason.NEEDS_REVIEW;
    }

    // Helper: Generate human-readable reason explanation
    private String generateReasonExplanation(TerminalReadPort.DecisionReason reason, double winRate, double drawdown) {
        return switch (reason) {
            case SOLID_PERFORMER ->
                String.format("↑%.1f%% win rate", winRate * 100);
            case NEEDS_REVIEW ->
                String.format("%.0f%% drawdown in 7 days", drawdown * 100);
            case HIGH_RISK ->
                String.format("%.0f%% critical drawdown", drawdown * 100);
            case SLIPPING ->
                "No recent signals";
        };
    }

    // Helper: Calculate drawdown for a subscription
    private double calculateDrawdown(UserSubscriptionEntity subscription) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<SignalEntity> signals = springDataSignalRepository.findByBotIdAndCreatedAtAfter(
                subscription.getBotId(),
                sevenDaysAgo
        );
        if (signals.isEmpty()) {
            return 0.0;
        }
        double minValue = signals.stream()
                .mapToDouble(s -> SignalMetricsCalculator.deriveReturn(toSignalData(s)))
                .min()
                .orElse(0.0);
        return Math.min(0.0, minValue);  // Return as negative value
    }

    // Helper: Calculate current P&L for a subscription
    private double calculateCurrentPnL(UserSubscriptionEntity subscription, UserPortfolioEntity portfolio) {
        List<SignalEntity> signals = springDataSignalRepository.findByBotIdAndCreatedAtAfter(
                subscription.getBotId(),
                LocalDateTime.now().minusDays(30)
        );
        if (signals.isEmpty()) {
            return 0.0;
        }
        double totalReturn = signals.stream()
                .mapToDouble(s -> SignalMetricsCalculator.deriveReturn(toSignalData(s)))
                .sum();
        return SignalMetricsCalculator.round2(totalReturn * portfolio.getTotalCapital().doubleValue());
    }

    // Helper: Days since subscription started
    private int daysSinceSubscribed(UserSubscriptionEntity subscription) {
        if (subscription.getCreatedAt() == null) {
            return 0;
        }
        return (int) java.time.Duration.between(subscription.getCreatedAt(), LocalDateTime.now()).toDays();
    }

    // Helper: Consecutive days with negative P&L
    private int daysInNegative(UserSubscriptionEntity subscription) {
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        List<SignalEntity> recentSignals = springDataSignalRepository.findByBotIdAndCreatedAtAfter(
                subscription.getBotId(),
                yesterday
        );
        long losingSignals = recentSignals.stream()
                .filter(s -> SignalMetricsCalculator.deriveReturn(toSignalData(s)) < 0)
                .count();
        return losingSignals > 0 ? 1 : 0;  // Simplified: 1 if any loss in last 24h
    }

    private SubscriptionStatus parseStatusFilter(String statusFilter) {
        if (statusFilter == null) {
            return null;
        }
        String normalized = statusFilter.trim().toUpperCase();
        if (normalized.isEmpty() || "ALL".equals(normalized)) {
            return null;
        }
        try {
            return SubscriptionStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            // Unknown filter falls back to "all" to avoid 500s from invalid query params.
            return null;
        }
    }

    private int reasonPriority(TerminalReadPort.DecisionReason reason) {
        return switch (reason) {
            case HIGH_RISK ->
                0;
            case NEEDS_REVIEW ->
                1;
            case SLIPPING ->
                2;
            case SOLID_PERFORMER ->
                3;
        };
    }

    private UserPortfolioEntity findPortfolioOrDefault(String userId) {
        return springDataUserPortfolioRepository.findByUserId(userId)
                .orElseGet(() -> UserPortfolioEntity.builder()
                        .totalCapital(java.math.BigDecimal.valueOf(10000))
                        .availableBalance(java.math.BigDecimal.valueOf(10000))
                        .unrealizedPnl(java.math.BigDecimal.ZERO)
                        .realizedPnl(java.math.BigDecimal.ZERO)
                        .maxDrawdownThreshold(java.math.BigDecimal.valueOf(0.1000))
                        .mediumRiskThreshold(java.math.BigDecimal.valueOf(0.0500))
                        .build()
                );
    }
}
