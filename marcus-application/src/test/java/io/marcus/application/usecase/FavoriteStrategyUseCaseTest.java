package io.marcus.application.usecase;

import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteStrategyUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TerminalReadPort terminalReadPort;

    @InjectMocks
    private FavoriteStrategyUseCase favoriteStrategyUseCase;

    @Test
    void shouldFavoriteStrategyForTraderUser() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("user-1"));
        when(userRepository.existsByIdAndRole("user-1", Role.USER)).thenReturn(true);
        TerminalReadPort.FavoriteStrategySnapshot snapshot = new TerminalReadPort.FavoriteStrategySnapshot("strat-1", true);
        when(terminalReadPort.favoriteStrategy("user-1", "strat-1")).thenReturn(snapshot);

        TerminalReadPort.FavoriteStrategySnapshot result = favoriteStrategyUseCase.execute(" strat-1 ");

        assertEquals(snapshot, result);
        verify(terminalReadPort).favoriteStrategy("user-1", "strat-1");
    }

    @Test
    void shouldRejectNonTraderUsers() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("user-1"));
        when(userRepository.existsByIdAndRole("user-1", Role.USER)).thenReturn(false);

        assertThrows(ForbiddenOperationException.class, () -> favoriteStrategyUseCase.execute("strat-1"));

        verifyNoInteractions(terminalReadPort);
    }

    @Test
    void shouldRejectBlankStrategyId() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("user-1"));
        when(userRepository.existsByIdAndRole("user-1", Role.USER)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> favoriteStrategyUseCase.execute("   "));

        verifyNoInteractions(terminalReadPort);
    }
}