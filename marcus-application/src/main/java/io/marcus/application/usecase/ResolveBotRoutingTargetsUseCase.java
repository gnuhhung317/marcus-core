package io.marcus.application.usecase;

import io.marcus.application.dto.ResolveBotRoutingTargetsRequest;
import io.marcus.domain.port.BotSubscriberRoutingPort;
import io.marcus.domain.port.UserSessionRoutingPort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class ResolveBotRoutingTargetsUseCase {

    private final BotSubscriberRoutingPort botSubscriberRoutingPort;
    private final UserSessionRoutingPort userSessionRoutingPort;

    public ResolveBotRoutingTargetsUseCase(
            BotSubscriberRoutingPort botSubscriberRoutingPort,
            UserSessionRoutingPort userSessionRoutingPort
    ) {
        this.botSubscriberRoutingPort = botSubscriberRoutingPort;
        this.userSessionRoutingPort = userSessionRoutingPort;
    }

    public Set<String> execute(ResolveBotRoutingTargetsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Resolve bot routing targets request is required");
        }
        if (request.botId() == null || request.botId().isBlank()) {
            throw new IllegalArgumentException("Bot id is required");
        }

        Set<String> userIds = botSubscriberRoutingPort.findActiveSubscriberUserIdsByBotId(request.botId().trim());
        if (userIds == null || userIds.isEmpty()) {
            return Set.of();
        }

        Set<String> sanitizedUserIds = userIds.stream()
                .filter(userId -> userId != null && !userId.isBlank())
                .map(String::trim)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        if (sanitizedUserIds.isEmpty()) {
            return Set.of();
        }

        return userSessionRoutingPort.findServerIdsByUserIds(sanitizedUserIds);
    }
}