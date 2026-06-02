package io.marcus.application.usecase;

import io.marcus.application.dto.BotAnalyticsDtos.TelemetrySnapshot;
import io.marcus.domain.exception.BotNotFoundException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.port.BotTelemetryPort;
import io.marcus.domain.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetLatestBotTelemetryUseCase {

    private final BotRepository botRepository;
    private final BotTelemetryPort botTelemetryPort;

    public TelemetrySnapshot execute(String botId, String apiKey) {
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

        return botTelemetryPort.findLatest(normalizedBotId)
                .map(point -> new TelemetrySnapshot(
                        point.timestamp(),
                        point.equity(),
                        point.realizedPnl(),
                        point.unrealizedPnl()
                ))
                .orElse(null);
    }
}
