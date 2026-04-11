package io.marcus.application.usecase;

import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetBotDetailUseCase {

    private final TerminalReadPort terminalReadPort;

    public TerminalReadPort.BotDetailSnapshot execute(String botId) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }
        return terminalReadPort.getBotDetail(botId.trim());
    }
}