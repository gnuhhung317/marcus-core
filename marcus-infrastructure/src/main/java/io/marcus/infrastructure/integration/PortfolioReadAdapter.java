package io.marcus.infrastructure.integration;

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
import io.marcus.infrastructure.persistence.SpringDataRawEventRepository;
import io.marcus.infrastructure.persistence.SpringDataPortfolioAggregateHistoryRepository;
import io.marcus.infrastructure.persistence.entity.RawEventEntity;
import io.marcus.infrastructure.persistence.entity.UserPortfolioEntity;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import lombok.RequiredArgsConstructor;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PortfolioReadAdapter implements PortfolioReadPort {

    private final SpringDataBotRepository springDataBotRepository;
    private final SpringDataSignalRepository springDataSignalRepository;
    private final SpringDataUserSubscriptionRepository springDataUserSubscriptionRepository;
    private final SpringDataUserPortfolioRepository springDataUserPortfolioRepository;
    private final SpringDataRawEventRepository springDataRawEventRepository;
    private final SpringDataPortfolioAggregateHistoryRepository springDataPortfolioAggregateHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TimeSeriesPointSnapshot> listDashboardEquitySeries(String userId, String range) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = resolveRangeStart(range, now);

        List<io.marcus.infrastructure.persistence.entity.PortfolioAggregateHistoryEntity> history =
                springDataPortfolioAggregateHistoryRepository.findByUserIdAndSnapshotAtAfterOrderBySnapshotAtAsc(userId, from);
        UserPortfolioEntity portfolio = currentPortfolioState(userId);
        double currentEquity = SignalMetricsCalculator.round4(safeDouble(portfolio.getTotalCapital(), 10000.0));

        if (history.isEmpty()) {
            return List.of(
                new TimeSeriesPointSnapshot(from, SignalMetricsCalculator.round4(currentEquity)),
                new TimeSeriesPointSnapshot(now, SignalMetricsCalculator.round4(currentEquity))
            );
        }

        List<TimeSeriesPointSnapshot> points = history.stream()
                .map(h -> new TimeSeriesPointSnapshot(h.getSnapshotAt(), SignalMetricsCalculator.round4(h.getTotal().doubleValue())))
                .collect(Collectors.toList());

        TimeSeriesPointSnapshot firstPoint = points.get(0);
        if (firstPoint.timestamp().isAfter(from)) {
            points.add(0, new TimeSeriesPointSnapshot(from, firstPoint.value()));
        }

        TimeSeriesPointSnapshot lastPoint = points.get(points.size() - 1);
        if (lastPoint.timestamp().isBefore(now)) {
            points.add(new TimeSeriesPointSnapshot(now, currentEquity));
        }

        return resampleDashboardEquitySeries(points, range);
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
                        signal.getGeneratedTimestamp(),
                        isSimulated(signal)
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
                        signal.getGeneratedTimestamp(),
                        isSimulated(signal)
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
                        signal.getGeneratedTimestamp(),
                        isSimulated(signal)
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

        List<UserSubscriptionEntity> subs = springDataUserSubscriptionRepository.findAll();
        int activeSubscribers = (int) subs.stream()
                .filter(s -> s.getBotId().equals(botId) && s.getStatus() == SubscriptionStatus.ACTIVE)
                .count();

        LocalDateTime lastSignalAt = springDataSignalRepository.findByBotIdOrderByGeneratedTimestampDesc(botId, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(SignalEntity::getGeneratedTimestamp)
                .orElse(null);

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
        return new ConnectivityHealthSnapshot("UNKNOWN", LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public BotIntegrationHealthSnapshot getBotIntegrationHealth(String botId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("botId must not be blank");
        }
        BotEntity bot = springDataBotRepository.findByBotId(botId)
                .orElseThrow(() -> new NoSuchElementException("Bot not found: " + botId));

        Optional<RawEventEntity> latestHeartbeatOpt = springDataRawEventRepository.findLatestHeartbeatForBot(bot.getBotId());

        LocalDateTime lastSignalAt = springDataSignalRepository.findByBotId(bot.getBotId())
                .stream()
                .map(SignalEntity::getGeneratedTimestamp)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        String wsStatus = "DOWN";
        if (latestHeartbeatOpt.isPresent()) {
            Instant receivedAt = latestHeartbeatOpt.get().getReceivedAt();
            Duration heartbeatAge = Duration.between(receivedAt, Instant.now());
            long ageSecs = heartbeatAge.getSeconds();
            if (ageSecs <= 330) {
                wsStatus = "UP";
            } else if (ageSecs <= 900) {
                wsStatus = "DEGRADED";
            } else {
                wsStatus = "DOWN";
            }
        }

        String overall;
        String message;
        if ("DOWN".equals(wsStatus)) {
            overall = "DOWN";
            message = "No heartbeat received within 15 minutes. Executor is offline.";
        } else if ("DEGRADED".equals(wsStatus)) {
            overall = "DEGRADED";
            message = "Executor heartbeat latency is high.";
        } else {
            overall = "UP";
            message = "System is fully healthy and active.";
        }

        return new BotIntegrationHealthSnapshot(overall, LocalDateTime.now(), lastSignalAt, message);
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
    @Transactional(readOnly = true)
    public ExecutionLogPageSnapshot listSystemExecutionLogs(String cursor, int limit) {
        int offset = 0;
        if (cursor != null && !cursor.isBlank()) {
            try {
                offset = Integer.parseInt(cursor.trim());
            } catch (NumberFormatException e) {
                // Keep default 0 offset if unparseable
            }
        }

        int fetchLimit = limit + 1;
        List<RawEventEntity> entities = springDataRawEventRepository.findSystemExecutionLogs(fetchLimit, offset);

        boolean hasMore = entities.size() > limit;
        List<RawEventEntity> pageEntities = hasMore ? entities.subList(0, limit) : entities;

        List<ExecutionLogItemSnapshot> items = pageEntities.stream()
                .map(entity -> {
                    LocalDateTime timestamp = LocalDateTime.ofInstant(entity.getReceivedAt(), ZoneId.systemDefault());
                    String level = determineLogLevel(entity);
                    String source = entity.getBotId() != null && !entity.getBotId().isBlank() ? entity.getBotId() : "system";
                    String message = formatLogMessage(entity);
                    return new ExecutionLogItemSnapshot(timestamp, level, source, message);
                })
                .collect(Collectors.toList());

        String nextCursor = hasMore ? String.valueOf(offset + limit) : null;
        return new ExecutionLogPageSnapshot(nextCursor, items);
    }

    private String determineLogLevel(RawEventEntity entity) {
        if ("error".equalsIgnoreCase(entity.getType())) {
            return "ERROR";
        }
        Map<String, Object> payload = entity.getPayload();
        if (payload != null) {
            Object status = payload.get("status");
            if (status != null && ("FAILED".equalsIgnoreCase(status.toString()) || "ERROR".equalsIgnoreCase(status.toString()))) {
                return "ERROR";
            }
            if (payload.containsKey("error") || payload.containsKey("errorMessage")) {
                return "ERROR";
            }
        }
        return "INFO";
    }

    private String formatLogMessage(RawEventEntity entity) {
        String type = entity.getType();
        Map<String, Object> payload = entity.getPayload();
        if (payload == null) {
            return "Event [" + type + "] received. CorrelationID: " + entity.getCorrelationId();
        }
        try {
            if ("ingest".equalsIgnoreCase(type)) {
                Object action = payload.get("action");
                Object symbol = payload.get("symbol");
                Object price = payload.get("price");
                if (action != null && symbol != null) {
                    return "Signal Ingested: " + action + " " + symbol + (price != null ? " @ " + price : "") + " (EventID: " + entity.getEventId() + ")";
                }
            } else if ("audit-push".equalsIgnoreCase(type)) {
                Object kind = payload.get("kind");
                if (kind != null) {
                    return "Audit Push: " + kind + " received (Conn: " + entity.getSourceConnId() + ")";
                }
            } else if ("heartbeat".equalsIgnoreCase(type)) {
                return "Heartbeat received (Conn: " + entity.getSourceConnId() + ", Seq: " + entity.getSequenceNo() + ")";
            } else if ("ack".equalsIgnoreCase(type)) {
                return "Acknowledgment received for EventID: " + payload.get("ackEventId") + " (Status: " + payload.get("status") + ")";
            }
        } catch (Exception e) {
            // Fall through to default formatting
        }
        return "Event [" + type + "] processed. Correlation: " + entity.getCorrelationId() + ", EventID: " + entity.getEventId();
    }

    @Override
    @Transactional(readOnly = true)
    public PortfolioOverviewSnapshot getPortfolioOverview(String userId) {
        List<UserSubscriptionEntity> activeSubscriptions = springDataUserSubscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);

        UserPortfolioEntity portfolio = currentPortfolioState(userId);
        int activeBotsCount = activeSubscriptions.size();

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

        double liveEquity = safeDouble(portfolio.getTotalCapital(), 0.0);
        double freeBalance = safeDouble(portfolio.getAvailableBalance(), liveEquity);
        double capitalInPositions = Math.max(0.0, liveEquity - freeBalance);

        return new PortfolioOverviewSnapshot(
                activeBotsCount,
                capitalInPositions,
                aggregateWinRate24h,
                atRiskCount,
                liveEquity,
                aggregateOpenPnL,
                portfolio.getLastSyncAt() != null ? portfolio.getLastSyncAt() : LocalDateTime.now(),
                portfolio.getFreshAccountsCount() != null ? portfolio.getFreshAccountsCount() : 0,
                portfolio.getStaleAccountsCount() != null ? portfolio.getStaleAccountsCount() : 0,
                portfolio.getDataFreshness() != null ? portfolio.getDataFreshness() : "STALE"
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
        UserPortfolioEntity portfolio = currentPortfolioState(subscription.getUserId());

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
                "",
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

    private LocalDateTime resolveRangeStart(String range, LocalDateTime now) {
        return switch (range != null ? range.trim().toUpperCase(Locale.ROOT) : "7D") {
            case "1D" -> now.minusDays(1);
            case "7D", "1W" -> now.minusDays(7);
            case "30D", "1M" -> now.minusDays(30);
            case "ALL" -> now.minusYears(10);
            default -> now.minusDays(7);
        };
    }

    private List<TimeSeriesPointSnapshot> resampleDashboardEquitySeries(List<TimeSeriesPointSnapshot> points, String range) {
        if (points.size() <= 3) {
            return points;
        }

        Duration bucketSize = resolveDashboardEquityBucketSize(range);
        long bucketMillis = bucketSize.toMillis();
        if (bucketMillis <= 0) {
            return points;
        }

        LocalDateTime firstTimestamp = points.get(0).timestamp();
        Map<Long, List<TimeSeriesPointSnapshot>> buckets = new LinkedHashMap<>();
        for (TimeSeriesPointSnapshot point : points) {
            long offsetMillis = Math.max(0, Duration.between(firstTimestamp, point.timestamp()).toMillis());
            long bucketKey = offsetMillis / bucketMillis;
            buckets.computeIfAbsent(bucketKey, ignored -> new ArrayList<>()).add(point);
        }

        List<TimeSeriesPointSnapshot> sampled = new ArrayList<>();
        for (List<TimeSeriesPointSnapshot> bucket : buckets.values()) {
            appendBucketShape(sampled, bucket);
        }

        TimeSeriesPointSnapshot firstPoint = points.get(0);
        if (sampled.isEmpty() || !sampled.get(0).timestamp().equals(firstPoint.timestamp())) {
            sampled.add(0, firstPoint);
        }

        TimeSeriesPointSnapshot lastPoint = points.get(points.size() - 1);
        appendDistinctPoint(sampled, lastPoint);

        return sampled;
    }

    private Duration resolveDashboardEquityBucketSize(String range) {
        return switch (range != null ? range.trim().toUpperCase(Locale.ROOT) : "7D") {
            case "1D" -> Duration.ofHours(1);
            case "7D", "1W" -> Duration.ofHours(4);
            case "30D", "1M" -> Duration.ofDays(1);
            case "ALL" -> Duration.ofDays(30);
            default -> Duration.ofHours(4);
        };
    }

    private void appendBucketShape(List<TimeSeriesPointSnapshot> sampled, List<TimeSeriesPointSnapshot> bucket) {
        if (bucket.isEmpty()) {
            return;
        }

        TimeSeriesPointSnapshot firstPoint = bucket.get(0);
        TimeSeriesPointSnapshot lastPoint = bucket.get(bucket.size() - 1);
        TimeSeriesPointSnapshot minPoint = firstPoint;
        TimeSeriesPointSnapshot maxPoint = firstPoint;

        for (TimeSeriesPointSnapshot point : bucket) {
            if (point.value() < minPoint.value()) {
                minPoint = point;
            }
            if (point.value() > maxPoint.value()) {
                maxPoint = point;
            }
        }

        List.of(firstPoint, minPoint, maxPoint, lastPoint).stream()
                .sorted(Comparator.comparing(TimeSeriesPointSnapshot::timestamp))
                .forEach(point -> appendDistinctPoint(sampled, point));
    }

    private void appendDistinctPoint(List<TimeSeriesPointSnapshot> sampled, TimeSeriesPointSnapshot point) {
        if (sampled.isEmpty()) {
            sampled.add(point);
            return;
        }

        int lastIndex = sampled.size() - 1;
        if (sampled.get(lastIndex).timestamp().equals(point.timestamp())) {
            sampled.set(lastIndex, point);
        } else {
            sampled.add(point);
        }
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

    private UserPortfolioEntity currentPortfolioState(String userId) {
        return springDataUserPortfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("Portfolio not found for user: " + userId));
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

    private boolean isSimulated(SignalEntity signal) {
        if (signal == null || signal.getMetadata() == null) {
            return false;
        }
        Object simulationVal = signal.getMetadata().get("simulation");
        if (simulationVal instanceof Boolean) {
            return (Boolean) simulationVal;
        }
        if (simulationVal instanceof String) {
            return Boolean.parseBoolean((String) simulationVal);
        }
        return false;
    }
}
