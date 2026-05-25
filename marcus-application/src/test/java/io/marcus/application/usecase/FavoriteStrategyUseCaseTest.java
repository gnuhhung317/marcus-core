package io.marcus.application.usecase;

import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.domain.port.BotDiscoveryReadPort;
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
    private BotDiscoveryReadPort botDiscoveryReadPort;

    @InjectMocks
    private FavoriteStrategyUseCase favoriteStrategyUseCase;

    @Test
    void shouldFavoriteStrategyForTraderUser() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("user-1"));
        when(userRepository.existsByIdAndRole("user-1", Role.TRADER)).thenReturn(true);
        BotDiscoveryReadPort.FavoriteStrategySnapshot snapshot = new BotDiscoveryReadPort.FavoriteStrategySnapshot("strat-1", true);
        when(botDiscoveryReadPort.favoriteStrategy("user-1", "strat-1")).thenReturn(snapshot);

        BotDiscoveryReadPort.FavoriteStrategySnapshot result = favoriteStrategyUseCase.execute(" strat-1 ");

        assertEquals(snapshot, result);
        verify(botDiscoveryReadPort).favoriteStrategy("user-1", "strat-1");
    }

    @Test
    void shouldRejectNonTraderUsers() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("user-1"));
        when(userRepository.existsByIdAndRole("user-1", Role.TRADER)).thenReturn(false);

        assertThrows(ForbiddenOperationException.class, () -> favoriteStrategyUseCase.execute("strat-1"));

        verifyNoInteractions(botDiscoveryReadPort);
    }

    @Test
    void shouldRejectBlankStrategyId() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("user-1"));
        when(userRepository.existsByIdAndRole("user-1", Role.TRADER)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> favoriteStrategyUseCase.execute("   "));

        verifyNoInteractions(botDiscoveryReadPort);
    }
}
