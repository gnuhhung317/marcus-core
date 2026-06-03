package io.marcus.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.application.dto.BotTelemetryRequest;
import io.marcus.domain.exception.BotNotFoundException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.BotTelemetryPoint;
import io.marcus.domain.port.BotTelemetryPort;
import io.marcus.domain.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SyncBotTelemetryUseCase {

    private final BotRepository botRepository;
    private final BotTelemetryPort botTelemetryPort;
    private final ObjectMapper objectMapper;

    public BotTelemetryPoint execute(String botId, String apiKey, BotTelemetryRequest request) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("botId is required");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("telemetry request is required");
        }

        String normalizedBotId = botId.trim();
        Bot bot = botRepository.findByBotId(normalizedBotId)
                .orElseThrow(() -> new BotNotFoundException("Bot not found: " + normalizedBotId));
        if (!apiKey.trim().equals(bot.getApiKey())) {
            throw new IllegalArgumentException("API Key mismatch for bot: " + normalizedBotId);
        }

        return botTelemetryPort.save(new BotTelemetryPoint(
                normalizedBotId,
                request.timestamp() != null ? request.timestamp() : LocalDateTime.now(),
                request.equity() != null ? request.equity() : BigDecimal.ZERO,
                request.realizedPnl() != null ? request.realizedPnl() : BigDecimal.ZERO,
                request.unrealizedPnl() != null ? request.unrealizedPnl() : BigDecimal.ZERO,
                metricsJson(request)
        ));
    }

    private String metricsJson(BotTelemetryRequest request) {
        try {
            return objectMapper.writeValueAsString(request.metrics() == null ? Map.of() : request.metrics());
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid telemetry metrics payload", ex);
        }
    }
}
