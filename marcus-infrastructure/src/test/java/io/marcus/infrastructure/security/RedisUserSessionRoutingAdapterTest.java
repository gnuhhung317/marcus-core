package io.marcus.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisUserSessionRoutingAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisUserSessionRoutingAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RedisUserSessionRoutingAdapter(stringRedisTemplate);
    }

    @Test
    void shouldUpsertSessionMapping() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        adapter.upsertSession("user-1", "session-1", "ws-1");

        verify(setOperations).add("marcus:user:user-1:sessions", "session-1");
        verify(valueOperations).set("marcus:session:session-1:server", "ws-1");
    }

    @Test
    void shouldRemoveSessionMapping() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);

        adapter.removeSession("user-1", "session-1");

        verify(setOperations).remove("marcus:user:user-1:sessions", "session-1");
        verify(stringRedisTemplate).delete("marcus:session:session-1:server");
    }

    @Test
    void shouldResolveUniqueServerIdsByUsers() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        Set<String> user1Sessions = new LinkedHashSet<>();
        user1Sessions.add("s1");
        user1Sessions.add("s2");

        when(setOperations.members("marcus:user:user-1:sessions")).thenReturn(user1Sessions);
        when(setOperations.members("marcus:user:user-2:sessions")).thenReturn(Set.of("s3"));
        when(valueOperations.multiGet(anyList())).thenAnswer(invocation -> {
            List<String> keys = invocation.getArgument(0);
            return keys.stream()
                    .map(key -> {
                        if ("marcus:session:s1:server".equals(key)) {
                            return "ws-1";
                        }
                        if ("marcus:session:s2:server".equals(key)) {
                            return "";
                        }
                        if ("marcus:session:s3:server".equals(key)) {
                            return "ws-2";
                        }
                        return null;
                    })
                    .toList();
        });

        Set<String> result = adapter.findServerIdsByUserIds(Set.of("user-1", "user-2"));

        assertThat(result).containsExactlyInAnyOrder("ws-1", "ws-2");
    }

    @Test
    void shouldReturnEmptyWhenUserIdsEmpty() {
        Set<String> result = adapter.findServerIdsByUserIds(Set.of());

        assertThat(result).isEmpty();
        verifyNoInteractions(setOperations, valueOperations);
    }

    @Test
    void shouldFallbackToLocalSessionWhenRedisMissingSessionData() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        adapter.upsertSession("user-1", "session-1", "ws-local");
        when(setOperations.members("marcus:user:user-1:sessions")).thenReturn(Set.of());

        Set<String> result = adapter.findServerIdsByUserIds(Set.of("user-1"));

        assertThat(result).containsExactly("ws-local");
    }

    @Test
    void shouldFallbackToLocalSessionWhenRedisLookupFails() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        adapter.upsertSession("user-1", "session-1", "ws-local");
        when(setOperations.members("marcus:user:user-1:sessions"))
                .thenThrow(new RuntimeException("redis unavailable"));

        Set<String> result = adapter.findServerIdsByUserIds(Set.of("user-1"));

        assertThat(result).containsExactly("ws-local");
    }

    @Test
    void shouldRemoveSessionFromLocalFallbackStore() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        adapter.upsertSession("user-1", "session-1", "ws-local");
        adapter.removeSession("user-1", "session-1");

        when(setOperations.members("marcus:user:user-1:sessions")).thenReturn(Set.of());

        Set<String> result = adapter.findServerIdsByUserIds(Set.of("user-1"));

        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotFailWhenRedisUnavailableDuringUpsertAndRemove() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("redis unavailable"))
                .when(setOperations)
                .add("marcus:user:user-1:sessions", "session-1");

        adapter.upsertSession("user-1", "session-1", "ws-local");

        Set<String> resultAfterUpsert = adapter.findServerIdsByUserIds(Set.of("user-1"));
        assertThat(resultAfterUpsert).containsExactly("ws-local");

        adapter.removeSession("user-1", "session-1");
        Set<String> resultAfterRemove = adapter.findServerIdsByUserIds(Set.of("user-1"));

        assertThat(resultAfterRemove).isEmpty();
    }
}
