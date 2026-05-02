package io.marcus.infrastructure.security;

import io.marcus.domain.port.BotSubscriberRoutingPort;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class RedisBotSubscriberRoutingAdapter implements BotSubscriberRoutingPort {

    private static final String BOT_SUBSCRIBERS_KEY_TEMPLATE = "marcus:bot:%s:subscribers";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisBotSubscriberRoutingAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void upsertSubscriber(String botId, String userId) {
        SetOperations<String, String> setOperations = stringRedisTemplate.opsForSet();
        setOperations.add(botSubscribersKey(botId), userId);
    }

    @Override
    public void removeSubscriber(String botId, String userId) {
        SetOperations<String, String> setOperations = stringRedisTemplate.opsForSet();
        setOperations.remove(botSubscribersKey(botId), userId);
    }

    @Override
    public Set<String> findActiveSubscriberUserIdsByBotId(String botId) {
        SetOperations<String, String> setOperations = stringRedisTemplate.opsForSet();
        Set<String> rawMembers = setOperations.members(botSubscribersKey(botId));

        if (rawMembers == null || rawMembers.isEmpty()) {
            return Set.of();
        }

        return rawMembers.stream()
                .filter(userId -> userId != null && !userId.isBlank())
                .map(String::trim)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    private String botSubscribersKey(String botId) {
        return BOT_SUBSCRIBERS_KEY_TEMPLATE.formatted(botId);
    }
}