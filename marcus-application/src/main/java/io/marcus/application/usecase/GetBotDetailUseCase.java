package io.marcus.application.usecase;

import io.marcus.domain.port.BotDiscoveryReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetBotDetailUseCase {

    private final BotDiscoveryReadPort botDiscoveryReadPort;

    public BotDiscoveryReadPort.BotDetailSnapshot execute(String botId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }
        return botDiscoveryReadPort.getBotDetail(botId.trim());
    }
}
