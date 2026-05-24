package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.PortfolioReadPort;
import io.marcus.domain.service.IdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDashboardEquitySeriesUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private PortfolioReadPort portfolioReadPort;

    private GetDashboardEquitySeriesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetDashboardEquitySeriesUseCase(identityService, portfolioReadPort);
    }

    @Test
    void shouldReturnEquitySeriesForCurrentUser() {
        List<PortfolioReadPort.TimeSeriesPointSnapshot> points = List.of(
                new PortfolioReadPort.TimeSeriesPointSnapshot(LocalDateTime.of(2026, 4, 1, 10, 0), 100.0)
        );

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(portfolioReadPort.listDashboardEquitySeries("usr_1", "1M")).thenReturn(points);

        List<PortfolioReadPort.TimeSeriesPointSnapshot> result = useCase.execute("1M");

        assertThat(result).containsExactlyElementsOf(points);
    }

    @Test
    void shouldThrowWhenNoAuthenticatedUser() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("1M"))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");
    }
}
