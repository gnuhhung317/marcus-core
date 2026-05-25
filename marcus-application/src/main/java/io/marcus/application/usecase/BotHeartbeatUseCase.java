package io.marcus.application.usecase;

import io.marcus.domain.exception.BotNotFoundException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.RawEvent;
import io.marcus.domain.port.RawEventPersistencePort;
import io.marcus.domain.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotHeartbeatUseCase {

    private final BotRepository botRepository;
    private final RawEventPersistencePort rawEventPersistencePort;

    public void execute(String botId, String apiKey) {
        if (botId == null || botId.isBlank()) {
            throw new IllegalArgumentException("botId is required");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("apiKey is required");
        }

        Bot bot = botRepository.findByBotId(botId.trim())
                .orElseThrow(() -> new BotNotFoundException("Bot not found: " + botId));

        if (!apiKey.trim().equals(bot.getApiKey())) {
            throw new IllegalArgumentException("API Key mismatch for bot: " + botId);
        }

        try {
            String eventId = UUID.randomUUID().toString();
            RawEvent rawEvent = new RawEvent();
            rawEvent.setEventId(eventId);
            rawEvent.setBotId(botId.trim());
            rawEvent.setIdempotencyKey("hb-key-" + eventId);
            rawEvent.setCorrelationId("hb-corr-" + eventId);
            rawEvent.setType("heartbeat");

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("timestamp", Instant.now().toString());
            rawEvent.setPayload(payloadMap);
            rawEvent.setReceivedAt(Instant.now());
            rawEvent.setSourceConnId("http-endpoint");
            rawEvent.setProcessed(true);
            rawEvent.setProcessedAt(Instant.now());

            rawEventPersistencePort.save(rawEvent);
            log.info("[Heartbeat] HTTP Heartbeat registered for botId={}", botId);
        } catch (Exception e) {
            log.error("[Heartbeat] Failed to persist HTTP heartbeat raw event for botId={}: {}", botId, e.getMessage(), e);
        }
    }
}
