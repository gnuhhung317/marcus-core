package io.marcus.application.usecase;

import io.marcus.application.dto.RemoveBotSubscriberRequest;
import io.marcus.domain.port.BotSubscriberRoutingPort;
import org.springframework.stereotype.Service;

@Service
public class RemoveBotSubscriberUseCase {

    private final BotSubscriberRoutingPort botSubscriberRoutingPort;

    public RemoveBotSubscriberUseCase(BotSubscriberRoutingPort botSubscriberRoutingPort) {
        this.botSubscriberRoutingPort = botSubscriberRoutingPort;
    }

    public void execute(RemoveBotSubscriberRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Remove bot subscriber request is required");
        }
        if (request.botId() == null || request.botId().isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }
        if (request.userId() == null || request.userId().isBlank()) {
            throw new IllegalArgumentException("User id is required");
        }

        botSubscriberRoutingPort.removeSubscriber(request.botId().trim(), request.userId().trim());
    }
}