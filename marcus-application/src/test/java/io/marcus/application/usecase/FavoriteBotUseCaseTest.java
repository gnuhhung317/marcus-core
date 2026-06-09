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
class FavoriteBotUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BotDiscoveryReadPort botDiscoveryReadPort;

    @InjectMocks
    private FavoriteBotUseCase favoriteBotUseCase;

    @Test
    void shouldFavoriteStrategyForTraderUser() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("user-1"));
        when(userRepository.existsByIdAndRole("user-1", Role.TRADER)).thenReturn(true);
        BotDiscoveryReadPort.FavoriteBotSnapshot snapshot = new BotDiscoveryReadPort.FavoriteBotSnapshot("bot-1", true);
        when(botDiscoveryReadPort.favoriteBot("user-1", "bot-1")).thenReturn(snapshot);

        BotDiscoveryReadPort.FavoriteBotSnapshot result = favoriteBotUseCase.execute(" bot-1 ");

        assertEquals(snapshot, result);
        verify(botDiscoveryReadPort).favoriteBot("user-1", "bot-1");
    }

    @Test
    void shouldRejectNonTraderUsers() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("user-1"));
        when(userRepository.existsByIdAndRole("user-1", Role.TRADER)).thenReturn(false);

        assertThrows(ForbiddenOperationException.class, () -> favoriteBotUseCase.execute("bot-1"));

        verifyNoInteractions(botDiscoveryReadPort);
    }

    @Test
    void shouldRejectBlankBotId() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("user-1"));
        when(userRepository.existsByIdAndRole("user-1", Role.TRADER)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> favoriteBotUseCase.execute("   "));

        verifyNoInteractions(botDiscoveryReadPort);
    }
}
