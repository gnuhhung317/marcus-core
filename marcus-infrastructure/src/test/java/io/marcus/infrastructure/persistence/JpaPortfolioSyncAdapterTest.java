package io.marcus.infrastructure.persistence;

import io.marcus.domain.port.PortfolioBalanceSyncData;
import io.marcus.domain.port.PortfolioSyncContext;
import io.marcus.infrastructure.persistence.entity.PortfolioAccountEntity;
import io.marcus.infrastructure.persistence.entity.PortfolioAggregateHistoryEntity;
import io.marcus.infrastructure.persistence.entity.PortfolioBalanceHistoryEntity;
import io.marcus.infrastructure.persistence.entity.UserPortfolioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaPortfolioSyncAdapterTest {

    @Mock
    private SpringDataUserPortfolioRepository springDataUserPortfolioRepository;
    @Mock
    private SpringDataPortfolioAccountRepository springDataPortfolioAccountRepository;
    @Mock
    private SpringDataPortfolioHistoryRepository springDataPortfolioHistoryRepository;
    @Mock
    private SpringDataPortfolioAggregateHistoryRepository springDataPortfolioAggregateHistoryRepository;

    private JpaPortfolioSyncAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaPortfolioSyncAdapter(
                springDataUserPortfolioRepository,
                springDataPortfolioAccountRepository,
                springDataPortfolioHistoryRepository,
                springDataPortfolioAggregateHistoryRepository
        );
    }

    @Test
    void syncBalance_serializesAggregateAndExcludesStaleAccounts() {
        LocalDateTime now = LocalDateTime.now();
        UserPortfolioEntity aggregate = UserPortfolioEntity.builder()
                .id("port-1")
                .userId("usr-1")
                .totalCapital(new BigDecimal("10000"))
                .availableBalance(new BigDecimal("10000"))
                .realizedPnl(BigDecimal.ZERO)
                .unrealizedPnl(BigDecimal.ZERO)
                .maxDrawdownThreshold(new BigDecimal("0.1000"))
                .mediumRiskThreshold(new BigDecimal("0.0500"))
                .freshAccountsCount(0)
                .staleAccountsCount(0)
                .dataFreshness("STALE")
                .build();

        PortfolioAccountEntity freshAccount = PortfolioAccountEntity.builder()
                .id("acc-1")
                .userId("usr-1")
                .userSubscriptionId("sub-1")
                .botId("bot-1")
                .wsToken("ws_1")
                .exchangeId("binance")
                .currency("USDT")
                .executionMode("live")
                .total(new BigDecimal("12000"))
                .free(new BigDecimal("7000"))
                .used(new BigDecimal("5000"))
                .realizedPnl(BigDecimal.ZERO)
                .unrealizedPnl(new BigDecimal("250"))
                .lastSyncAt(now.minusHours(2))
                .active(true)
                .build();
        PortfolioAccountEntity staleAccount = PortfolioAccountEntity.builder()
                .id("acc-2")
                .userId("usr-1")
                .userSubscriptionId("sub-2")
                .botId("bot-2")
                .wsToken("ws_2")
                .exchangeId("bybit")
                .currency("USDT")
                .executionMode("live")
                .total(new BigDecimal("5000"))
                .free(new BigDecimal("5000"))
                .used(BigDecimal.ZERO)
                .realizedPnl(BigDecimal.ZERO)
                .unrealizedPnl(BigDecimal.ZERO)
                .lastSyncAt(now.minusHours(25))
                .active(true)
                .build();

        when(springDataUserPortfolioRepository.findByUserIdForUpdate("usr-1")).thenReturn(Optional.of(aggregate));
        when(springDataPortfolioAccountRepository.findByUserSubscriptionId("sub-1")).thenReturn(Optional.of(freshAccount));
        when(springDataPortfolioAccountRepository.findByUserId("usr-1")).thenReturn(List.of(freshAccount, staleAccount));

        adapter.syncBalance(
                new PortfolioSyncContext("usr-1", "sub-1", "bot-1", "ws_1"),
                new PortfolioBalanceSyncData(
                        new BigDecimal("12000"),
                        new BigDecimal("7000"),
                        new BigDecimal("5000"),
                        new BigDecimal("250"),
                        "binance",
                        "USDT",
                        "live"
                )
        );

        verify(springDataUserPortfolioRepository).findByUserIdForUpdate("usr-1");
        verify(springDataPortfolioAccountRepository).save(any(PortfolioAccountEntity.class));
        verify(springDataPortfolioHistoryRepository).save(any(PortfolioBalanceHistoryEntity.class));

        ArgumentCaptor<UserPortfolioEntity> aggregateCaptor = ArgumentCaptor.forClass(UserPortfolioEntity.class);
        verify(springDataUserPortfolioRepository).save(aggregateCaptor.capture());
        assertThat(aggregateCaptor.getValue().getTotalCapital()).isEqualByComparingTo("12000");
        assertThat(aggregateCaptor.getValue().getAvailableBalance()).isEqualByComparingTo("7000");
        assertThat(aggregateCaptor.getValue().getUnrealizedPnl()).isEqualByComparingTo("250");
        assertThat(aggregateCaptor.getValue().getFreshAccountsCount()).isEqualTo(1);
        assertThat(aggregateCaptor.getValue().getStaleAccountsCount()).isEqualTo(1);
        assertThat(aggregateCaptor.getValue().getDataFreshness()).isEqualTo("PARTIAL");

        ArgumentCaptor<PortfolioAggregateHistoryEntity> historyCaptor = ArgumentCaptor.forClass(PortfolioAggregateHistoryEntity.class);
        verify(springDataPortfolioAggregateHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getTotal()).isEqualByComparingTo("12000");
        assertThat(historyCaptor.getValue().getFreshAccountsCount()).isEqualTo(1);
        assertThat(historyCaptor.getValue().getStaleAccountsCount()).isEqualTo(1);
        assertThat(historyCaptor.getValue().getDataFreshness()).isEqualTo("PARTIAL");
    }

    @Test
    void syncBalance_missingAggregateThrowsInsteadOfCreatingInTransaction() {
        when(springDataUserPortfolioRepository.findByUserIdForUpdate("usr-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.syncBalance(
                new PortfolioSyncContext("usr-1", "sub-1", "bot-1", "ws_1"),
                new PortfolioBalanceSyncData(
                        new BigDecimal("12000"),
                        new BigDecimal("7000"),
                        new BigDecimal("5000"),
                        new BigDecimal("250"),
                        "binance",
                        "USDT",
                        "live"
                )
        )).isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("Portfolio aggregate missing for userId=usr-1");
    }

    @Test
    void syncBalance_alwaysPersistsRecentAccountSnapshot() {
        LocalDateTime now = LocalDateTime.now();
        UserPortfolioEntity aggregate = UserPortfolioEntity.builder()
                .id("port-1")
                .userId("usr-1")
                .totalCapital(new BigDecimal("10000"))
                .availableBalance(new BigDecimal("10000"))
                .realizedPnl(BigDecimal.ZERO)
                .unrealizedPnl(BigDecimal.ZERO)
                .maxDrawdownThreshold(new BigDecimal("0.1000"))
                .mediumRiskThreshold(new BigDecimal("0.0500"))
                .freshAccountsCount(0)
                .staleAccountsCount(0)
                .dataFreshness("STALE")
                .build();

        PortfolioAccountEntity recentAccount = PortfolioAccountEntity.builder()
                .id("acc-1")
                .userId("usr-1")
                .userSubscriptionId("sub-1")
                .botId("bot-1")
                .wsToken("ws_1")
                .exchangeId("binance")
                .currency("USDT")
                .executionMode("live")
                .total(new BigDecimal("12000"))
                .free(new BigDecimal("7000"))
                .used(new BigDecimal("5000"))
                .realizedPnl(BigDecimal.ZERO)
                .unrealizedPnl(new BigDecimal("250"))
                .lastSyncAt(now.minusMinutes(10))
                .active(true)
                .build();

        when(springDataUserPortfolioRepository.findByUserIdForUpdate("usr-1")).thenReturn(Optional.of(aggregate));
        when(springDataPortfolioAccountRepository.findByUserSubscriptionId("sub-1")).thenReturn(Optional.of(recentAccount));

        adapter.syncBalance(
                new PortfolioSyncContext("usr-1", "sub-1", "bot-1", "ws_1"),
                new PortfolioBalanceSyncData(
                        new BigDecimal("12000"),
                        new BigDecimal("7000"),
                        new BigDecimal("5000"),
                        new BigDecimal("250"),
                        "binance",
                        "USDT",
                        "live"
                )
        );

        ArgumentCaptor<PortfolioAccountEntity> accountCaptor = ArgumentCaptor.forClass(PortfolioAccountEntity.class);
        verify(springDataPortfolioAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getLastSyncAt()).isNotNull();
        assertThat(accountCaptor.getValue().getLastSyncAt()).isAfter(now.minusMinutes(1));

        verify(springDataPortfolioHistoryRepository).save(any(PortfolioBalanceHistoryEntity.class));
        verify(springDataPortfolioAggregateHistoryRepository).save(any(PortfolioAggregateHistoryEntity.class));
    }
}
