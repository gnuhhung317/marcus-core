package io.marcus.infrastructure.persistence;

import io.marcus.domain.port.PortfolioBalanceSyncData;
import io.marcus.domain.port.PortfolioSyncContext;
import io.marcus.domain.port.PortfolioSyncPort;
import io.marcus.infrastructure.persistence.entity.PortfolioAccountEntity;
import io.marcus.infrastructure.persistence.entity.PortfolioAggregateHistoryEntity;
import io.marcus.infrastructure.persistence.entity.PortfolioBalanceHistoryEntity;
import io.marcus.infrastructure.persistence.entity.UserPortfolioEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Repository
@RequiredArgsConstructor
public class JpaPortfolioSyncAdapter implements PortfolioSyncPort {

    private static final int STALE_HOURS = 24;

    private final SpringDataUserPortfolioRepository springDataUserPortfolioRepository;
    private final SpringDataPortfolioAccountRepository springDataPortfolioAccountRepository;
    private final SpringDataPortfolioHistoryRepository springDataPortfolioHistoryRepository;
    private final SpringDataPortfolioAggregateHistoryRepository springDataPortfolioAggregateHistoryRepository;

    @Override
    @Transactional
    public void syncBalance(PortfolioSyncContext context, PortfolioBalanceSyncData data) {
        if (context == null || context.userId() == null || context.userId().isBlank()) {
            return;
        }
        if (context.userSubscriptionId() == null || context.userSubscriptionId().isBlank()) {
            return;
        }
        if (data == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime freshnessCutoff = now.minusHours(STALE_HOURS);
        UserPortfolioEntity aggregate = lockExistingAggregate(context.userId());
        PortfolioAccountEntity account = upsertAccount(context, data, now);

        saveAccountHistory(context, data, now);

        List<PortfolioAccountEntity> allAccounts = springDataPortfolioAccountRepository.findByUserId(context.userId());
        List<PortfolioAccountEntity> freshAccounts = allAccounts.stream()
                .filter(portfolioAccount -> isFresh(portfolioAccount, freshnessCutoff))
                .toList();

        BigDecimal total = freshAccounts.stream().map(PortfolioAccountEntity::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal free = freshAccounts.stream().map(PortfolioAccountEntity::getFree).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal used = freshAccounts.stream().map(PortfolioAccountEntity::getUsed).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal realizedPnl = freshAccounts.stream().map(PortfolioAccountEntity::getRealizedPnl).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unrealizedPnl = freshAccounts.stream().map(PortfolioAccountEntity::getUnrealizedPnl).reduce(BigDecimal.ZERO, BigDecimal::add);

        int freshCount = freshAccounts.size();
        int staleCount = Math.max(0, allAccounts.size() - freshCount);
        String freshness = freshCount == 0 ? "STALE" : (staleCount > 0 ? "PARTIAL" : "FRESH");

        aggregate.setTotalCapital(total);
        aggregate.setAvailableBalance(free);
        aggregate.setRealizedPnl(realizedPnl);
        aggregate.setUnrealizedPnl(unrealizedPnl);
        aggregate.setExchangeId(data.exchangeId() != null && !data.exchangeId().isBlank()
                ? data.exchangeId()
                : account.getExchangeId());
        aggregate.setLastSyncAt(now);
        aggregate.setFreshAccountsCount(freshCount);
        aggregate.setStaleAccountsCount(staleCount);
        aggregate.setDataFreshness(freshness);
        springDataUserPortfolioRepository.save(aggregate);

        springDataPortfolioAggregateHistoryRepository.save(
                PortfolioAggregateHistoryEntity.builder()
                        .userId(context.userId())
                        .total(total)
                        .free(free)
                        .used(used)
                        .realizedPnl(realizedPnl)
                        .unrealizedPnl(unrealizedPnl)
                        .freshAccountsCount(freshCount)
                        .staleAccountsCount(staleCount)
                        .dataFreshness(freshness)
                        .exchangeId(aggregate.getExchangeId())
                        .snapshotAt(now)
                        .build()
        );
    }

    private UserPortfolioEntity lockExistingAggregate(String userId) {
        return springDataUserPortfolioRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Portfolio aggregate missing for userId=" + userId + ". Initialize portfolio before syncing balances."
                ));
    }

    private PortfolioAccountEntity upsertAccount(PortfolioSyncContext context, PortfolioBalanceSyncData data, LocalDateTime now) {
        PortfolioAccountEntity account = springDataPortfolioAccountRepository.findByUserSubscriptionId(context.userSubscriptionId())
                .orElseGet(PortfolioAccountEntity::new);

        account.setUserId(context.userId());
        account.setUserSubscriptionId(context.userSubscriptionId());
        account.setBotId(context.botId());
        account.setWsToken(context.wsToken());
        account.setExchangeId(normalize(data.exchangeId()));
        account.setCurrency(normalize(data.currency()));
        account.setExecutionMode(normalize(data.executionMode()));
        account.setTotal(safe(data.total()));
        account.setFree(safe(data.available()));
        account.setUsed(safe(data.used()));
        account.setRealizedPnl(BigDecimal.ZERO);
        account.setUnrealizedPnl(safe(data.unrealizedPnl()));
        account.setLastSyncAt(now);
        account.setActive(true);
        return springDataPortfolioAccountRepository.save(account);
    }

    private void saveAccountHistory(PortfolioSyncContext context, PortfolioBalanceSyncData data, LocalDateTime now) {
        springDataPortfolioHistoryRepository.save(
                PortfolioBalanceHistoryEntity.builder()
                        .userId(context.userId())
                        .userSubscriptionId(context.userSubscriptionId())
                        .botId(context.botId())
                        .total(safe(data.total()))
                        .free(safe(data.available()))
                        .used(safe(data.used()))
                        .unrealizedPnl(safe(data.unrealizedPnl()))
                        .exchangeId(normalize(data.exchangeId()))
                        .currency(normalize(data.currency()))
                        .executionMode(normalize(data.executionMode()))
                        .active(true)
                        .snapshotAt(now)
                        .build()
        );
    }

    private boolean isFresh(PortfolioAccountEntity account, LocalDateTime freshnessCutoff) {
        return account != null
                && account.isActive()
                && account.getLastSyncAt() != null
                && !account.getLastSyncAt().isBefore(freshnessCutoff);
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
