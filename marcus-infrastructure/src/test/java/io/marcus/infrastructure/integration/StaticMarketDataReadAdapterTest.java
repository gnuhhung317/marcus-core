package io.marcus.infrastructure.integration;

import io.marcus.domain.port.MarketDataReadPort.ExchangeAllocationSnapshot;
import io.marcus.infrastructure.persistence.SpringDataBotRepository;
import io.marcus.infrastructure.persistence.SpringDataPortfolioAccountRepository;
import io.marcus.infrastructure.persistence.SpringDataSignalRepository;
import io.marcus.infrastructure.persistence.SpringDataUserPortfolioRepository;
import io.marcus.infrastructure.persistence.SpringDataUserSubscriptionRepository;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import io.marcus.infrastructure.persistence.entity.ExchangeEntity;
import io.marcus.infrastructure.persistence.entity.PortfolioAccountEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaticMarketDataReadAdapterTest {

    @Mock
    private SpringDataBotRepository springDataBotRepository;
    @Mock
    private SpringDataSignalRepository springDataSignalRepository;
    @Mock
    private SpringDataUserSubscriptionRepository springDataUserSubscriptionRepository;
    @Mock
    private SpringDataUserPortfolioRepository springDataUserPortfolioRepository;
    @Mock
    private SpringDataPortfolioAccountRepository springDataPortfolioAccountRepository;

    private StaticMarketDataReadAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new StaticMarketDataReadAdapter(
                springDataBotRepository,
                springDataSignalRepository,
                springDataUserSubscriptionRepository,
                springDataUserPortfolioRepository,
                springDataPortfolioAccountRepository
        );
    }

    @Test
    void listExchangeAllocation_returnsPercentPointsForFreshActiveCapital() {
        when(springDataBotRepository.findAllWithExchange()).thenReturn(List.of(
                bot("bot-binance", "BINANCE"),
                bot("bot-bybit", "BYBIT")
        ));
        when(springDataPortfolioAccountRepository.findByUserId("usr-1")).thenReturn(List.of(
                account("usr-1", "bot-binance", "binance-sub", "600", true, LocalDateTime.now().minusMinutes(10)),
                account("usr-1", "bot-bybit", "bybit-sub", "400", true, LocalDateTime.now().minusMinutes(10))
        ));

        List<ExchangeAllocationSnapshot> allocation = adapter.listExchangeAllocation("usr-1");

        assertEquals(2, allocation.size());
        assertEquals("BINANCE", allocation.get(0).exchange());
        assertEquals(60.0, allocation.get(0).percentage());
        assertEquals("BYBIT", allocation.get(1).exchange());
        assertEquals(40.0, allocation.get(1).percentage());
    }

    private BotEntity bot(String botId, String exchangeId) {
        return BotEntity.builder()
                .botId(botId)
                .exchange(ExchangeEntity.builder()
                        .exchangeId(exchangeId)
                        .name(exchangeId)
                        .build())
                .build();
    }

    private PortfolioAccountEntity account(
            String userId,
            String botId,
            String subscriptionId,
            String total,
            boolean active,
            LocalDateTime lastSyncAt
    ) {
        return PortfolioAccountEntity.builder()
                .userId(userId)
                .botId(botId)
                .userSubscriptionId(subscriptionId)
                .wsToken(subscriptionId + "-token")
                .total(new BigDecimal(total))
                .free(BigDecimal.ZERO)
                .used(BigDecimal.ZERO)
                .realizedPnl(BigDecimal.ZERO)
                .unrealizedPnl(BigDecimal.ZERO)
                .lastSyncAt(lastSyncAt)
                .active(active)
                .build();
    }
}
