package io.marcus.application.usecase;

import io.marcus.application.dto.AdminDtos;
import io.marcus.domain.model.Bot;
import io.marcus.domain.port.AdminBotPort;
import io.marcus.domain.port.AdminSubscriptionPort;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.BotStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUpdateBotStatusUseCaseTest {

    @Mock
    private AdminBotPort adminBotPort;

    @Mock
    private AdminSubscriptionPort adminSubscriptionPort;

    @Mock
    private IdentityService identityService;

    @Mock
    private AdminRecordAuditEventUseCase adminRecordAuditEventUseCase;

    private AdminUpdateBotStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AdminUpdateBotStatusUseCase(
                adminBotPort,
                adminSubscriptionPort,
                identityService,
                adminRecordAuditEventUseCase
        );
    }

    @Test
    void shouldUpdateBotStatusSafelyEvenWithNullFields() {
        // Create a bot with null fields (e.g. tradingPair, exchangeId, status)
        Bot bot = Bot.builder()
                .botId("bot_test")
                .name("Test Bot")
                .developerId("dev_1")
                .status(null)
                .tradingPair(null)
                .exchangeId(null)
                .build();

        AdminDtos.UpdateBotStatusRequest request = new AdminDtos.UpdateBotStatusRequest(
                BotStatus.DELETED,
                "cleanup",
                false
        );

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("admin_1"));
        when(adminBotPort.findByBotId("bot_test")).thenReturn(Optional.of(bot));
        when(adminBotPort.save(any(Bot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminDtos.BotRow result = useCase.execute("bot_test", request);

        assertThat(result.botId()).isEqualTo("bot_test");
        assertThat(result.status()).isEqualTo(BotStatus.DELETED.name());

        verify(adminBotPort).save(bot);
        verify(adminRecordAuditEventUseCase).execute(
                eq("admin_1"),
                eq("BOT_STATUS_UPDATED"),
                eq("BOT"),
                eq("bot_test"),
                eq("cleanup"),
                any(),
                any()
        );
    }
}
