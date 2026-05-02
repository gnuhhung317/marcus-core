package io.marcus.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisBotSubscriberRoutingAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    private RedisBotSubscriberRoutingAdapter adapter;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        adapter = new RedisBotSubscriberRoutingAdapter(stringRedisTemplate);
    }

    @Test
    void shouldUpsertAndRemoveSubscriber() {
        adapter.upsertSubscriber("bot-1", "user-1");
        adapter.removeSubscriber("bot-1", "user-1");

        verify(setOperations).add("marcus:bot:bot-1:subscribers", "user-1");
        verify(setOperations).remove("marcus:bot:bot-1:subscribers", "user-1");
    }

    @Test
    void shouldResolveSanitizedSubscriberUserIds() {
        LinkedHashSet<String> rawMembers = new LinkedHashSet<>();
        rawMembers.add(" user-1 ");
        rawMembers.add("");
        rawMembers.add(null);
        rawMembers.add("user-2");

        when(setOperations.members("marcus:bot:bot-1:subscribers")).thenReturn(rawMembers);

        Set<String> result = adapter.findActiveSubscriberUserIdsByBotId("bot-1");

        assertThat(result).containsExactly("user-1", "user-2");
    }

    @Test
    void shouldReturnEmptyWhenNoSubscribers() {
        when(setOperations.members("marcus:bot:bot-1:subscribers")).thenReturn(Set.of());

        Set<String> result = adapter.findActiveSubscriberUserIdsByBotId("bot-1");

        assertThat(result).isEmpty();
    }
}
