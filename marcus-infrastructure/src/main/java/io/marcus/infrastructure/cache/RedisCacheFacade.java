package io.marcus.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Component
@Slf4j
public class RedisCacheFacade {

    private static final int SCAN_COUNT = 1_000;
    private static final int DELETE_BATCH_SIZE = 500;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final String keyPrefix;

    public RedisCacheFacade(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${marcus.cache.enabled:true}") boolean enabled,
            @Value("${marcus.cache.key-prefix:marcus:cache:v1}") String keyPrefix
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.keyPrefix = stripTrailingColon(keyPrefix);
    }

    public <T> T getOrLoad(String key, Duration ttl, TypeReference<T> typeReference, Supplier<T> loader) {
        if (!enabled) {
            return loader.get();
        }

        String redisKey = fullKey(key);
        try {
            String cached = redisTemplate.opsForValue().get(redisKey);
            if (cached != null) {
                return objectMapper.readValue(cached, typeReference);
            }
        } catch (Exception ex) {
            log.warn("[RedisCache] read failed for key={}: {}", redisKey, ex.getMessage());
        }

        T loaded = loader.get();
        put(key, loaded, ttl);
        return loaded;
    }

    public void put(String key, Object value, Duration ttl) {
        if (!enabled || value == null) {
            return;
        }

        String redisKey = fullKey(key);
        try {
            String payload = objectMapper.writeValueAsString(value);
            if (ttl == null || ttl.isZero() || ttl.isNegative()) {
                redisTemplate.opsForValue().set(redisKey, payload);
            } else {
                redisTemplate.opsForValue().set(redisKey, payload, ttl);
            }
        } catch (Exception ex) {
            log.warn("[RedisCache] write failed for key={}: {}", redisKey, ex.getMessage());
        }
    }

    public void evict(String key) {
        if (!enabled) {
            return;
        }

        String redisKey = fullKey(key);
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception ex) {
            log.warn("[RedisCache] evict failed for key={}: {}", redisKey, ex.getMessage());
        }
    }

    public void evictByPrefix(String prefix) {
        if (!enabled) {
            return;
        }

        String normalizedPrefix = trimWildcard(prefix);
        String match = fullKey(normalizedPrefix) + "*";
        try {
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                deleteByScan(connection, match);
                return null;
            });
        } catch (Exception ex) {
            log.warn("[RedisCache] prefix evict failed for match={}: {}", match, ex.getMessage());
        }
    }

    private void deleteByScan(RedisConnection connection, String match) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(match)
                .count(SCAN_COUNT)
                .build();

        List<byte[]> batch = new ArrayList<>(DELETE_BATCH_SIZE);
        try (Cursor<byte[]> cursor = connection.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= DELETE_BATCH_SIZE) {
                    deleteBatch(connection, batch);
                }
            }
            deleteBatch(connection, batch);
        }
    }

    private void deleteBatch(RedisConnection connection, List<byte[]> batch) {
        if (batch.isEmpty()) {
            return;
        }
        connection.del(batch.toArray(byte[][]::new));
        batch.clear();
    }

    private String fullKey(String key) {
        String normalizedKey = key == null ? "" : key.trim();
        if (normalizedKey.startsWith(keyPrefix + ":")) {
            return normalizedKey;
        }
        return keyPrefix + ":" + normalizedKey;
    }

    private String stripTrailingColon(String value) {
        String normalized = value == null || value.isBlank() ? "marcus:cache:v1" : value.trim();
        while (normalized.endsWith(":")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String trimWildcard(String prefix) {
        String normalized = prefix == null ? "" : prefix.trim();
        while (normalized.endsWith("*")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public static String keyPart(String value) {
        if (value == null || value.isBlank()) {
            return "_";
        }
        return java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.trim().getBytes(StandardCharsets.UTF_8));
    }
}
