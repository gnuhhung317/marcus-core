package io.marcus.infrastructure.security;

import io.marcus.domain.port.ExecutorOnlineStatusPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Redis TTL-based implementation of {@link ExecutorOnlineStatusPort}.
 *
 * <p>Key pattern: {@code marcus:executor:online:{wsToken}}
 * <ul>
 *   <li>Set on connect / heartbeat with a rolling TTL (typically 30 s).</li>
 *   <li>Deleted explicitly on clean disconnect.</li>
 *   <li>Automatically expires if executor crashes without a clean close.</li>
 * </ul>
 */
@Component
@Slf4j
public class RedisExecutorOnlineStatusAdapter implements ExecutorOnlineStatusPort {

    private static final String ONLINE_KEY_TEMPLATE = "marcus:executor:online:%s";
    private static final String ONLINE_KEY_PATTERN = "marcus:executor:online:*";
    private static final String ONLINE_MARKER = "1";

    private final StringRedisTemplate redisTemplate;

    public RedisExecutorOnlineStatusAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void markOnline(String wsToken, long ttlSeconds) {
        if (wsToken == null || wsToken.isBlank()) return;
        try {
            redisTemplate.opsForValue()
                    .set(onlineKey(wsToken), ONLINE_MARKER, Duration.ofSeconds(ttlSeconds));
        } catch (RuntimeException e) {
            log.warn("[ExecutorOnline] Redis write failed for wsToken={}: {}", wsToken, e.getMessage());
        }
    }

    @Override
    public void markOffline(String wsToken) {
        if (wsToken == null || wsToken.isBlank()) return;
        try {
            redisTemplate.delete(onlineKey(wsToken));
        } catch (RuntimeException e) {
            log.warn("[ExecutorOnline] Redis delete failed for wsToken={}: {}", wsToken, e.getMessage());
        }
    }

    @Override
    public boolean isOnline(String wsToken) {
        if (wsToken == null || wsToken.isBlank()) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(onlineKey(wsToken)));
        } catch (RuntimeException e) {
            log.warn("[ExecutorOnline] Redis check failed for wsToken={}: {}", wsToken, e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isAnyOnline() {
        try (Cursor<String> cursor = redisTemplate.scan(
                ScanOptions.scanOptions().match(ONLINE_KEY_PATTERN).count(1).build())) {
            return cursor.hasNext();
        } catch (RuntimeException e) {
            log.warn("[ExecutorOnline] Redis scan failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Set<String> filterOnline(Set<String> wsTokens) {
        if (wsTokens == null || wsTokens.isEmpty()) return Set.of();
        Set<String> online = new HashSet<>();
        for (String token : wsTokens) {
            if (isOnline(token)) {
                online.add(token);
            }
        }
        return online;
    }

    private String onlineKey(String wsToken) {
        return ONLINE_KEY_TEMPLATE.formatted(wsToken);
    }
}
