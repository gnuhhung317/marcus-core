package io.marcus.infrastructure.security;

import io.marcus.domain.port.UserSessionRoutingPort;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RedisUserSessionRoutingAdapter implements UserSessionRoutingPort {

    private static final String USER_SESSIONS_KEY_TEMPLATE = "marcus:user:%s:sessions";
    private static final String SESSION_SERVER_KEY_TEMPLATE = "marcus:session:%s:server";

    private final StringRedisTemplate stringRedisTemplate;
    private final Map<String, Set<String>> localSessionsByUser = new ConcurrentHashMap<>();
    private final Map<String, String> localServerBySession = new ConcurrentHashMap<>();

    public RedisUserSessionRoutingAdapter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void upsertSession(String userId, String sessionId, String serverId) {
        localSessionsByUser
                .computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(sessionId);
        localServerBySession.put(sessionId, serverId);

        try {
            SetOperations<String, String> setOperations = stringRedisTemplate.opsForSet();
            ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
            setOperations.add(userSessionsKey(userId), sessionId);
            valueOperations.set(sessionServerKey(sessionId), serverId);
        } catch (RuntimeException ignored) {
            // Keep local mapping so this node can still route while Redis is unavailable.
        }
    }

    @Override
    public void removeSession(String userId, String sessionId) {
        Set<String> localSessionIds = localSessionsByUser.get(userId);
        if (localSessionIds != null) {
            localSessionIds.remove(sessionId);
            if (localSessionIds.isEmpty()) {
                localSessionsByUser.remove(userId);
            }
        }
        localServerBySession.remove(sessionId);

        try {
            SetOperations<String, String> setOperations = stringRedisTemplate.opsForSet();
            setOperations.remove(userSessionsKey(userId), sessionId);
            stringRedisTemplate.delete(sessionServerKey(sessionId));
        } catch (RuntimeException ignored) {
            // Removal already applied locally; Redis cleanup can be retried by subsequent updates.
        }
    }

    @Override
    public Set<String> findServerIdsByUserIds(Set<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Set.of();
        }

        Set<String> serverIds = new HashSet<>();
        Set<String> unresolvedUserIds = new LinkedHashSet<>();

        try {
            SetOperations<String, String> setOperations = stringRedisTemplate.opsForSet();
            ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();

            for (String userId : userIds) {
                Set<String> sessionIds = setOperations.members(userSessionsKey(userId));
                if (sessionIds == null || sessionIds.isEmpty()) {
                    unresolvedUserIds.add(userId);
                    continue;
                }

                List<String> sessionServerKeys = sessionIds.stream()
                        .map(this::sessionServerKey)
                        .toList();

                List<String> resolvedServerIds = valueOperations.multiGet(sessionServerKeys);
                if (resolvedServerIds == null || resolvedServerIds.isEmpty()) {
                    unresolvedUserIds.add(userId);
                    continue;
                }

                resolvedServerIds.stream()
                        .filter(serverId -> serverId != null && !serverId.isBlank())
                        .forEach(serverIds::add);
            }
        } catch (RuntimeException ignored) {
            unresolvedUserIds.addAll(userIds);
        }

        if (!unresolvedUserIds.isEmpty()) {
            serverIds.addAll(resolveLocalServerIds(unresolvedUserIds));
        }

        return serverIds;
    }

    private Set<String> resolveLocalServerIds(Set<String> userIds) {
        Set<String> serverIds = new LinkedHashSet<>();

        for (String userId : userIds) {
            Set<String> sessionIds = localSessionsByUser.getOrDefault(userId, Set.of());
            for (String sessionId : sessionIds) {
                String serverId = localServerBySession.get(sessionId);
                if (serverId != null && !serverId.isBlank()) {
                    serverIds.add(serverId);
                }
            }
        }

        return serverIds;
    }

    private String userSessionsKey(String userId) {
        return USER_SESSIONS_KEY_TEMPLATE.formatted(userId);
    }

    private String sessionServerKey(String sessionId) {
        return SESSION_SERVER_KEY_TEMPLATE.formatted(sessionId);
    }
}
