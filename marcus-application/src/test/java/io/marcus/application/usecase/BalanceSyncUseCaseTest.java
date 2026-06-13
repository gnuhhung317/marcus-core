package io.marcus.application.usecase;

import io.marcus.application.dto.BalanceSyncRequest;
import io.marcus.domain.port.PortfolioBalanceSyncData;
import io.marcus.domain.port.PortfolioSyncContext;
import io.marcus.domain.port.PortfolioSyncPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BalanceSyncUseCaseTest {

    @Mock
    private PortfolioSyncPort portfolioSyncPort;

    private BalanceSyncUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BalanceSyncUseCase(portfolioSyncPort);
    }

    @Test
    void execute_mapsContextAndRequestIntoSyncPort() {
        PortfolioSyncContext context = new PortfolioSyncContext("usr_1", "sub_1", "bot_1", "ws_abc");
        BalanceSyncRequest request = new BalanceSyncRequest(
                new BigDecimal("12000"),
                new BigDecimal("8000"),
                new BigDecimal("4000"),
                new BigDecimal("-250"),
                "binance",
                "USDT",
                "live"
        );

        useCase.execute(context, request);

        ArgumentCaptor<PortfolioBalanceSyncData> captor = ArgumentCaptor.forClass(PortfolioBalanceSyncData.class);
        verify(portfolioSyncPort).syncBalance(org.mockito.ArgumentMatchers.eq(context), captor.capture());

        assertThat(captor.getValue().total()).isEqualByComparingTo("12000");
        assertThat(captor.getValue().available()).isEqualByComparingTo("8000");
        assertThat(captor.getValue().used()).isEqualByComparingTo("4000");
        assertThat(captor.getValue().unrealizedPnl()).isEqualByComparingTo("-250");
        assertThat(captor.getValue().exchangeId()).isEqualTo("binance");
        assertThat(captor.getValue().currency()).isEqualTo("USDT");
        assertThat(captor.getValue().executionMode()).isEqualTo("live");
    }
}
