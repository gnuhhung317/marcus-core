package io.marcus.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisCacheInvalidator {

    private final RedisCacheFacade cacheFacade;

    public void evictBotCatalog(String botId) {
        cacheFacade.evictByPrefix("marketplace:bots:");
        cacheFacade.evictByPrefix("leaderboard:");
        evictBotDetail(botId);
    }

    public void evictSubscriptionCatalog(String botId) {
        cacheFacade.evictByPrefix("marketplace:bots:");
        evictBotDetail(botId);
    }

    public void evictBotAnalyticsAndCatalog(String botId) {
        evictBotAnalytics(botId);
        evictBotCatalog(botId);
    }

    public void evictSignalDerivedCatalog(String botId) {
        cacheFacade.evictByPrefix("marketplace:bots:");
        evictBotDetail(botId);
    }

    public void evictBotAnalytics(String botId) {
        if (botId == null || botId.isBlank()) {
            cacheFacade.evictByPrefix("bot-analytics:");
            return;
        }
        String botPart = RedisCacheFacade.keyPart(botId);
        cacheFacade.evictByPrefix("bot-analytics:metrics:" + botPart);
        cacheFacade.evictByPrefix("bot-analytics:series:" + botPart + ":");
    }

    private void evictBotDetail(String botId) {
        if (botId == null || botId.isBlank()) {
            cacheFacade.evictByPrefix("marketplace:bot-detail:");
            return;
        }
        cacheFacade.evict("marketplace:bot-detail:" + RedisCacheFacade.keyPart(botId));
    }
}
