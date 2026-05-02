package io.marcus.application.usecase;

import io.marcus.application.dto.UpsertBotSubscriberRequest;
import io.marcus.domain.port.BotSubscriberRoutingPort;
import org.springframework.stereotype.Service;

@Service
public class UpsertBotSubscriberUseCase {

    private final BotSubscriberRoutingPort botSubscriberRoutingPort;

    public UpsertBotSubscriberUseCase(BotSubscriberRoutingPort botSubscriberRoutingPort) {
        this.botSubscriberRoutingPort = botSubscriberRoutingPort;
    }

    public void execute(UpsertBotSubscriberRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Upsert bot subscriber request is required");
        }
        if (request.botId() == null || request.botId().isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }
        if (request.userId() == null || request.userId().isBlank()) {
            throw new IllegalArgumentException("User id is required");
        }

        botSubscriberRoutingPort.upsertSubscriber(request.botId().trim(), request.userId().trim());
    }
}