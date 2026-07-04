package io.marcus.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisCacheFacadeTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisCacheFacade cacheFacade;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheFacade = new RedisCacheFacade(redisTemplate, new ObjectMapper().findAndRegisterModules(), true, "marcus:cache:v1");
    }

    @Test
    void getOrLoadReturnsCachedValueWithoutCallingLoader() {
        when(valueOperations.get("marcus:cache:v1:sample:key")).thenReturn("\"cached\"");
        AtomicBoolean loaderCalled = new AtomicBoolean(false);

        String result = cacheFacade.getOrLoad(
                "sample:key",
                Duration.ofSeconds(30),
                new TypeReference<>() {},
                () -> {
                    loaderCalled.set(true);
                    return "loaded";
                }
        );

        assertEquals("cached", result);
        assertFalse(loaderCalled.get());
        verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void getOrLoadWritesLoadedValueOnMiss() {
        when(valueOperations.get("marcus:cache:v1:sample:key")).thenReturn(null);

        String result = cacheFacade.getOrLoad(
                "sample:key",
                Duration.ofSeconds(30),
                new TypeReference<>() {},
                () -> "loaded"
        );

        assertEquals("loaded", result);
        verify(valueOperations).set("marcus:cache:v1:sample:key", "\"loaded\"", Duration.ofSeconds(30));
    }

    @Test
    void getOrLoadFallsBackToLoaderWhenRedisReadFails() {
        when(valueOperations.get("marcus:cache:v1:sample:key")).thenThrow(new RuntimeException("redis down"));

        String result = cacheFacade.getOrLoad(
                "sample:key",
                Duration.ofSeconds(30),
                new TypeReference<>() {},
                () -> "loaded"
        );

        assertEquals("loaded", result);
    }

    @Test
    void evictByPrefixUsesScanAndDeletesMatchedKeys() {
        RedisConnection connection = mock(RedisConnection.class);
        Cursor<byte[]> cursor = mock(Cursor.class);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn(
                "marcus:cache:v1:marketplace:bots:a".getBytes(StandardCharsets.UTF_8),
                "marcus:cache:v1:marketplace:bots:b".getBytes(StandardCharsets.UTF_8)
        );
        when(connection.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(redisTemplate.execute(any(RedisCallback.class))).thenAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            return callback.doInRedis(connection);
        });

        cacheFacade.evictByPrefix("marketplace:bots:");

        verify(connection).scan(any(ScanOptions.class));
        verify(connection).del(any(byte[][].class));
    }

    @Test
    void evictDoesNotThrowWhenRedisDeleteFails() {
        doThrow(new RuntimeException("redis down")).when(redisTemplate).delete(eq("marcus:cache:v1:sample:key"));

        cacheFacade.evict("sample:key");

        verify(redisTemplate).delete("marcus:cache:v1:sample:key");
    }
}
