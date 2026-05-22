package io.marcus.infrastructure.integration;

import io.marcus.domain.port.MarketDataReadPort;
import io.marcus.domain.service.SignalMetricsCalculator;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StaticMarketDataReadAdapter implements MarketDataReadPort {

    private final SpringDataBotRepository springDataBotRepository;
    private final SpringDataSignalRepository springDataSignalRepository;
    private final SpringDataUserSubscriptionRepository springDataUserSubscriptionRepository;
    private final SpringDataUserPortfolioRepository springDataUserPortfolioRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewSnapshot getDashboardOverview(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }

        UserPortfolioEntity portfolio = findPortfolioOrDefault(userId);

        List<UserSubscriptionEntity> activeSubscriptions = springDataUserSubscriptionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE);

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

        double totalEquity = SignalMetricsCalculator.round2(safeDouble(portfolio.getTotalCapital(), 0.0));
        double openPnl = SignalMetricsCalculator.round2(safeDouble(portfolio.getUnrealizedPnl(), 0.0));
        double winRate = signalDataList.isEmpty() ? 0.0
                : SignalMetricsCalculator.round4(successfulSignals / (double) signalDataList.size());
        int activeBots = activeSubscriptions.size();
        return new DashboardOverviewSnapshot(totalEquity, openPnl, winRate, activeBots);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeAllocationSnapshot> listExchangeAllocation(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }

        Map<String, String> exchangeByBotId = springDataBotRepository.findAllWithExchange()
                .stream()
                .collect(Collectors.toMap(
                        BotEntity::getBotId,
                        this::resolveExchangeLabel,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<UserSubscriptionEntity> activeSubscriptions = springDataUserSubscriptionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE);

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

    private UserPortfolioEntity findPortfolioOrDefault(String userId) {
        return springDataUserPortfolioRepository.findByUserId(userId)
                .orElseGet(() -> UserPortfolioEntity.builder()
                        .totalCapital(java.math.BigDecimal.valueOf(10000.0))
                        .availableBalance(java.math.BigDecimal.valueOf(10000.0))
                        .unrealizedPnl(java.math.BigDecimal.ZERO)
                        .realizedPnl(java.math.BigDecimal.ZERO)
                        .maxDrawdownThreshold(java.math.BigDecimal.valueOf(0.1000))
                        .mediumRiskThreshold(java.math.BigDecimal.valueOf(0.0500))
                        .build());
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

    private double safeDouble(java.math.BigDecimal value, double fallback) {
        return value != null ? value.doubleValue() : fallback;
    }
}
