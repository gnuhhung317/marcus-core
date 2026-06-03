package io.marcus.application.usecase;

import io.marcus.domain.exception.BotNotFoundException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.BotDryRunState;
import io.marcus.domain.port.BotDryRunPort;
import io.marcus.domain.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetLatestBotDryRunUseCase {

    private final BotRepository botRepository;
    private final BotDryRunPort botDryRunPort;

    public BotDryRunState execute(String botId, String apiKey) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("botId is required");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey is required");
        }

        String normalizedBotId = botId.trim();
        Bot bot = botRepository.findByBotId(normalizedBotId)
                .orElseThrow(() -> new BotNotFoundException("Bot not found: " + normalizedBotId));
        if (!apiKey.trim().equals(bot.getApiKey())) {
            throw new IllegalArgumentException("API Key mismatch for bot: " + normalizedBotId);
        }

        return botDryRunPort.findLatestState(normalizedBotId).orElse(null);
    }
}
