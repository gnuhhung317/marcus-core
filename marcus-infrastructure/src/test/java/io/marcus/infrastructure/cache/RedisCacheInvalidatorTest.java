package io.marcus.infrastructure.cache;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RedisCacheInvalidatorTest {

    @Test
    void evictBotCatalogClearsMarketplaceLeaderboardAndBotDetail() {
        RedisCacheFacade cacheFacade = mock(RedisCacheFacade.class);
        RedisCacheInvalidator invalidator = new RedisCacheInvalidator(cacheFacade);

        invalidator.evictBotCatalog("bot_123");

        verify(cacheFacade).evictByPrefix("marketplace:bots:");
        verify(cacheFacade).evictByPrefix("leaderboard:");
        verify(cacheFacade).evict("marketplace:bot-detail:" + RedisCacheFacade.keyPart("bot_123"));
    }

    @Test
    void evictBotAnalyticsAndCatalogClearsKnownAnalyticsPrefixes() {
        RedisCacheFacade cacheFacade = mock(RedisCacheFacade.class);
        RedisCacheInvalidator invalidator = new RedisCacheInvalidator(cacheFacade);

        invalidator.evictBotAnalyticsAndCatalog("bot_123");

        String botPart = RedisCacheFacade.keyPart("bot_123");
        verify(cacheFacade).evictByPrefix("bot-analytics:metrics:" + botPart);
        verify(cacheFacade).evictByPrefix("bot-analytics:series:" + botPart + ":");
        verify(cacheFacade).evictByPrefix("leaderboard:");
        verify(cacheFacade).evictByPrefix("marketplace:bots:");
    }
}
