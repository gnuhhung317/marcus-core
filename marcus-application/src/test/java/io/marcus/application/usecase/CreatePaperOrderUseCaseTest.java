package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.service.IdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreatePaperOrderUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private TerminalReadPort terminalReadPort;

    private CreatePaperOrderUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreatePaperOrderUseCase(identityService, terminalReadPort);
    }

    @Test
    void shouldCreateLimitOrderWhenPayloadValid() {
        TerminalReadPort.PaperOrderSnapshot expected = new TerminalReadPort.PaperOrderSnapshot("ord_1", "ACCEPTED", 64500.5);
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(terminalReadPort.createPaperOrder(any(), any())).thenReturn(expected);

        TerminalReadPort.PaperOrderSnapshot result = useCase.execute("BTCUSDT", "LIMIT", "BUY", 0.5, 64500.5);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void shouldRejectMarketOrderWithLimitPrice() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));

        assertThatThrownBy(() -> useCase.execute("BTCUSDT", "MARKET", "BUY", 1.0, 100.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limitPrice must be omitted for MARKET orders");
    }

    @Test
    void shouldRejectLimitOrderWithoutLimitPrice() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));

        assertThatThrownBy(() -> useCase.execute("BTCUSDT", "LIMIT", "BUY", 1.0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("limitPrice must be provided and greater than 0 for LIMIT orders");
    }

    @Test
    void shouldThrowWhenNoAuthenticatedUser() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("BTCUSDT", "MARKET", "BUY", 1.0, null))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");
    }
}
