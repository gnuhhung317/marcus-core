package io.marcus.infrastructure.integration;

import io.marcus.domain.model.UserPortfolio;
import io.marcus.domain.port.PortfolioReadPort;
import io.marcus.domain.service.SignalMetricsCalculator;
import io.marcus.domain.service.PortfolioAnalyzerService;
import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.infrastructure.persistence.SpringDataBotRepository;
import io.marcus.infrastructure.persistence.SpringDataSignalRepository;
import io.marcus.infrastructure.persistence.SpringDataUserSubscriptionRepository;
import io.marcus.infrastructure.persistence.SpringDataUserPortfolioRepository;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import io.marcus.infrastructure.persistence.entity.SignalEntity;
import io.marcus.infrastructure.persistence.entity.UserPortfolioEntity;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StaticPortfolioReadAdapter implements PortfolioReadPort {

    private final SpringDataBotRepository springDataBotRepository;
    private final SpringDataSignalRepository springDataSignalRepository;
    private final SpringDataUserSubscriptionRepository springDataUserSubscriptionRepository;
    private final SpringDataUserPortfolioRepository springDataUserPortfolioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TimeSeriesPointSnapshot> listDashboardEquitySeries(String userId, String range) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        UserPortfolioEntity portfolio = findPortfolioOrDefault(userId);
        double currentEquity = safeDouble(portfolio.getTotalCapital(), 10000.0);
        return List.of(new TimeSeriesPointSnapshot(LocalDateTime.now(), SignalMetricsCalculator.round4(currentEquity)));
    }

    @Override
    public PaperSessionSummarySnapshot getPaperSessionSummary(String userId) {
        throw new UnsupportedOperationException("getPaperSessionSummary is not implemented");
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
        throw new UnsupportedOperationException("createPaperOrder is not implemented");
    }

    @Override
    public PaperSessionStateSnapshot pausePaperSession(String userId) {
        throw new UnsupportedOperationException("pausePaperSession is not implemented");
    }

    @Override
    public PaperSessionStateSnapshot resumePaperSession(String userId) {
        throw new UnsupportedOperationException("resumePaperSession is not implemented");
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
    @Transactional(readOnly = true)
    public List<SignalItemSnapshot> listSignalsByBot(String botId, String status, int limit) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("botId must not be blank");
        }
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        String normalizedStatus = (status == null || status.isBlank()) ? "ALL" : status.trim().toUpperCase(Locale.ROOT);

        // For simplicity we just use findByBotIdOrderByGeneratedTimestampDesc and filter in memory if status != ALL
        // In a real system you'd add a method findByBotIdAndStatusStringOrderByGeneratedTimestampDesc
        List<SignalEntity> signals = springDataSignalRepository.findByBotIdOrderByGeneratedTimestampDesc(
                botId, PageRequest.of(0, normalizedLimit)
        );

        if (!"ALL".equals(normalizedStatus)) {
            signals = signals.stream()
                    .filter(s -> s.getStatus() != null && s.getStatus().name().equals(normalizedStatus))
                    .toList();
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
    @Transactional(readOnly = true)
    public List<SignalItemSnapshot> listSignalsBySignalId(String signalId) {
        if (signalId == null || signalId.isBlank()) {
            return List.of();
        }
        return springDataSignalRepository.findBySignalId(signalId)
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
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public BotSignalSummarySnapshot getBotSignalSummary(String botId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("botId must not be blank");
        }

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<SignalEntity> recent = springDataSignalRepository.findByBotIdAndCreatedAtAfter(botId, since);

        long totalSignals24h = recent.size();
        long success = recent.stream()
                .filter(s -> SignalMetricsCalculator.deriveReturn(toSignalData(s)) > 0)
                .count();

        // Calculate active subscribers
        // Using UserSubscription repository
        List<UserSubscriptionEntity> subs = springDataUserSubscriptionRepository.findAll();
        int activeSubscribers = (int) subs.stream()
                .filter(s -> s.getBotId().equals(botId) && s.getStatus() == SubscriptionStatus.ACTIVE)
                .count();

        LocalDateTime lastSignalAt = springDataSignalRepository.findByBotIdOrderByGeneratedTimestampDesc(botId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(SignalEntity::getGeneratedTimestamp)
                .orElse(null);

        // Mock dispatched total for now since we don't persist per-subscriber dispatches yet
        long totalDispatched24h = totalSignals24h * Math.max(1, activeSubscribers);
        double deliveryRate = totalDispatched24h > 0 ? ((double) (success * Math.max(1, activeSubscribers)) / totalDispatched24h) * 100 : 100.0;
        
        return new BotSignalSummarySnapshot(
                totalSignals24h,
                success,
                SignalMetricsCalculator.round4(deliveryRate),
                activeSubscribers,
                lastSignalAt
        );
    }

    @Override
    public ConnectivityHealthSnapshot getSystemConnectivityHealth() {
        return new ConnectivityHealthSnapshot("UNKNOWN", LocalDateTime.now(), List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public BotIntegrationHealthSnapshot getBotIntegrationHealth(String botId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("botId must not be blank");
        }
        BotEntity bot = springDataBotRepository.findByBotId(botId)
                .orElseThrow(() -> new NoSuchElementException("Bot not found: " + botId));

        List<ConnectivityHealthDependencySnapshot> deps = List.of(
                new ConnectivityHealthDependencySnapshot("Signal Router", "UP", 8),
                new ConnectivityHealthDependencySnapshot("Price Feed", "UP", 12),
                new ConnectivityHealthDependencySnapshot("Order Executor", "DEGRADED", 38)
        );

        LocalDateTime lastSignalAt = springDataSignalRepository.findByBotId(bot.getBotId())
                .stream()
                .map(SignalEntity::getGeneratedTimestamp)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        String overall = (lastSignalAt != null && lastSignalAt.isAfter(LocalDateTime.now().minusHours(1))) ? "UP" : "DEGRADED";
        String message = overall.equals("UP") ? "" : "No recent signal within 1 hour";

        return new BotIntegrationHealthSnapshot(overall, LocalDateTime.now(), deps, lastSignalAt, message);
    }

    @Override
    @Transactional(readOnly = true)
    public SubscriptionDeliverySummarySnapshot getSubscriptionDeliverySummary(String subscriptionId) {
        if (subscriptionId == null || subscriptionId.isBlank()) {
            throw new IllegalArgumentException("subscriptionId must not be blank");
        }
        UserSubscriptionEntity subscription = springDataUserSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new NoSuchElementException("Subscription not found: " + subscriptionId));

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<SignalEntity> recent = springDataSignalRepository.findByBotIdAndCreatedAtAfter(subscription.getBotId(), since);

        long success = recent.stream().filter(s -> SignalMetricsCalculator.deriveReturn(toSignalData(s)) > 0).count();
        long failure = recent.size() - success;

        LocalDateTime lastDeliveryAt = recent.stream()
                .map(SignalEntity::getGeneratedTimestamp)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        DeliveryErrorSnapshot lastError = null;
        if (failure > 0) {
            lastError = new DeliveryErrorSnapshot("E_DELIVERY_FAILURE", "Some executions failed in the last 24h");
        }

        return new SubscriptionDeliverySummarySnapshot(success, failure, lastDeliveryAt, lastError);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiKeySnapshot getBotCredentials(String botId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("botId must not be blank");
        }
        BotEntity bot = springDataBotRepository.findByBotId(botId)
                .orElseThrow(() -> new NoSuchElementException("Bot not found: " + botId));

        String apiKey = bot.getApiKey() == null ? "" : bot.getApiKey();
        String masked;
        if (apiKey.length() <= 4) {
            masked = "****";
        } else {
            int keep = Math.min(4, apiKey.length());
            masked = apiKey.substring(0, keep) + "*".repeat(Math.max(0, apiKey.length() - keep));
        }

        String apiKeyId = bot.getBotId() + "-key";
        return new ApiKeySnapshot(apiKeyId, masked, false);
    }

    @Override
    public ExecutionLogPageSnapshot listSystemExecutionLogs(String cursor, int limit) {
        return new ExecutionLogPageSnapshot(cursor, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioOverviewSnapshot getPortfolioOverview(String userId) {
        List<UserSubscriptionEntity> activeSubscriptions = springDataUserSubscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);

        UserPortfolioEntity portfolio = findPortfolioOrDefault(userId);
        int activeBotsCount = activeSubscriptions.size();
        double totalCapital = safeDouble(portfolio.getTotalCapital(), 10000.0);

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
                .filter(sub -> calculateDrawdown(sub) < -Math.abs(safeDouble(portfolio.getMaxDrawdownThreshold(), 0.1000)))
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

        DecisionReason reason = determineReason(winRate, drawdown, signals24h, failureRate, portfolio);
        String explanation = generateReasonExplanation(reason, winRate, drawdown);
        double riskScore = Math.max(0.0, Math.min(1.0, 1.0 + drawdown));

        BotEntity bot = springDataBotRepository.findByBotId(subscription.getBotId())
                .orElseThrow(() -> new NoSuchElementException("Bot not found: " + subscription.getBotId()));

        return new SubscriptionDecisionSnapshot(
                subscription.getId(),
                subscription.getBotId(),
                bot.getName(),
                "/api/icons/bot/" + subscription.getBotId() + ".png",
                subscription.getStatus().name(),
                currentPnL,
                currentPnL / Math.max(1.0, safeDouble(portfolio.getTotalCapital(), 10000.0)),
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

    private DecisionReason determineReason(
            double winRate,
            double drawdown,
            List<SignalEntity> signals,
            double failureRate,
            UserPortfolioEntity portfolio
    ) {
        double maxDrawdownThreshold = safeDouble(portfolio.getMaxDrawdownThreshold(), 0.1000);
        double mediumRiskThreshold = safeDouble(portfolio.getMediumRiskThreshold(), 0.0500);
        LocalDateTime fourHoursAgo = LocalDateTime.now().minusHours(4);
        boolean hasRecentSignals = !signals.isEmpty() && signals.stream()
                .anyMatch(s -> s.getGeneratedTimestamp() != null && s.getGeneratedTimestamp().isAfter(fourHoursAgo));

        return PortfolioAnalyzerService.determineReason(
                winRate,
                drawdown,
                hasRecentSignals,
                failureRate,
                maxDrawdownThreshold,
                mediumRiskThreshold
        );
    }

    private String generateReasonExplanation(DecisionReason reason, double winRate, double drawdown) {
        return PortfolioAnalyzerService.generateReasonExplanation(reason, winRate, drawdown);
    }

    private double calculateDrawdown(UserSubscriptionEntity subscription) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        List<SignalEntity> signals = springDataSignalRepository.findByBotIdAndCreatedAtAfter(
                subscription.getBotId(),
                sevenDaysAgo
        );
        List<SignalMetricsCalculator.SignalData> signalDataList = signals.stream().map(this::toSignalData).toList();
        return PortfolioAnalyzerService.calculateDrawdown(signalDataList);
    }

    private double calculateCurrentPnL(UserSubscriptionEntity subscription, UserPortfolioEntity portfolio) {
        List<SignalEntity> signals = springDataSignalRepository.findByBotIdAndCreatedAtAfter(
                subscription.getBotId(),
                LocalDateTime.now().minusDays(30)
        );
        List<SignalMetricsCalculator.SignalData> signalDataList = signals.stream().map(this::toSignalData).toList();
        double totalCapital = safeDouble(portfolio.getTotalCapital(), 10000.0);
        return PortfolioAnalyzerService.calculateCurrentPnL(signalDataList, totalCapital);
    }

    private int daysSinceSubscribed(UserSubscriptionEntity subscription) {
        if (subscription.getCreatedAt() == null) {
            return 0;
        }
        return (int) java.time.Duration.between(subscription.getCreatedAt(), LocalDateTime.now()).toDays();
    }

    private int daysInNegative(UserSubscriptionEntity subscription) {
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        List<SignalEntity> recentSignals = springDataSignalRepository.findByBotIdAndCreatedAtAfter(
                subscription.getBotId(),
                yesterday
        );
        long losingSignals = recentSignals.stream()
                .filter(s -> SignalMetricsCalculator.deriveReturn(toSignalData(s)) < 0)
                .count();
        return losingSignals > 0 ? 1 : 0;
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
            return null;
        }
    }

    private int reasonPriority(DecisionReason reason) {
        return switch (reason) {
            case HIGH_RISK -> 0;
            case NEEDS_REVIEW -> 1;
            case SLIPPING -> 2;
            case SOLID_PERFORMER -> 3;
        };
    }

    private UserPortfolioEntity findPortfolioOrDefault(String userId) {
        return springDataUserPortfolioRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserPortfolio defaultPortfolio = defaultPortfolio(userId);
                    return UserPortfolioEntity.builder()
                            .totalCapital(defaultPortfolio.getTotalCapital())
                            .availableBalance(defaultPortfolio.getAvailableBalance())
                            .unrealizedPnl(defaultPortfolio.getUnrealizedPnl())
                            .realizedPnl(defaultPortfolio.getRealizedPnl())
                            .maxDrawdownThreshold(defaultPortfolio.getMaxDrawdownThreshold())
                            .mediumRiskThreshold(defaultPortfolio.getMediumRiskThreshold())
                            .build();
                });
    }

    private UserPortfolio defaultPortfolio(String userId) {
        return UserPortfolio.createDefault(userId);
    }

    private double safeDouble(java.math.BigDecimal value, double fallback) {
        return value != null ? value.doubleValue() : fallback;
    }

    private SignalMetricsCalculator.SignalData toSignalData(SignalEntity signal) {
        return new SignalMetricsCalculator.SignalData(
                signal.getEntry(), signal.getTakeProfit(),
                signal.getStopLoss(), signal.getAction()
        );
    }

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
}
