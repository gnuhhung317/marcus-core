package io.marcus.domain.port;

import java.util.Set;

public interface BotSubscriberRoutingPort {

    void upsertSubscriber(String botId, String userId);

    void removeSubscriber(String botId, String userId);

    Set<String> findActiveSubscriberUserIdsByBotId(String botId);
}