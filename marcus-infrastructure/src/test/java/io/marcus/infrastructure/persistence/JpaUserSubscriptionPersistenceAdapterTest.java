package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import io.marcus.infrastructure.persistence.mapper.UserSubscriptionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaUserSubscriptionPersistenceAdapterTest {

    @Mock
    private SpringDataUserSubscriptionRepository springDataUserSubscriptionRepository;

    @Mock
    private UserSubscriptionMapper userSubscriptionMapper;

    private JpaUserSubscriptionPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaUserSubscriptionPersistenceAdapter(springDataUserSubscriptionRepository, userSubscriptionMapper);
    }

    @Test
    void shouldSaveSubscription() {
        UserSubscription domain = UserSubscription.builder().userSubscriptionId("sub_1").build();
        UserSubscriptionEntity mappedEntity = UserSubscriptionEntity.builder().userSubscriptionId("sub_1").build();
        UserSubscriptionEntity savedEntity = UserSubscriptionEntity.builder().userSubscriptionId("sub_1").build();
        UserSubscription expected = UserSubscription.builder().userSubscriptionId("sub_1").build();

        when(userSubscriptionMapper.toEntity(domain)).thenReturn(mappedEntity);
        when(springDataUserSubscriptionRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(userSubscriptionMapper.toDomain(savedEntity)).thenReturn(expected);

        UserSubscription actual = adapter.save(domain);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void shouldFindActiveByUserAndBotId() {
        UserSubscriptionEntity entity = UserSubscriptionEntity.builder().userId("usr_1").botId("bot_1").build();
        UserSubscription domain = UserSubscription.builder().userId("usr_1").botId("bot_1").build();

        when(springDataUserSubscriptionRepository.findByUserIdAndBotIdAndStatus("usr_1", "bot_1", SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(entity));
        when(userSubscriptionMapper.toDomain(entity)).thenReturn(domain);

        Optional<UserSubscription> result = adapter.findActiveByUserIdAndBotId("usr_1", "bot_1");

        assertThat(result).contains(domain);
    }

    @Test
    void shouldFindActiveSubscriptionsByUserId() {
        UserSubscriptionEntity entity = UserSubscriptionEntity.builder().userId("usr_1").botId("bot_1").build();
        UserSubscription domain = UserSubscription.builder().userId("usr_1").botId("bot_1").build();

        when(springDataUserSubscriptionRepository.findByUserIdAndStatusOrderByCreatedAtDesc("usr_1", SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(entity));
        when(userSubscriptionMapper.toDomain(entity)).thenReturn(domain);

        List<UserSubscription> result = adapter.findActiveByUserId("usr_1");

        assertThat(result).containsExactly(domain);
    }

    @Test
    void shouldFindAnyActiveWsTokenByUserId() {
        UserSubscriptionEntity entity = UserSubscriptionEntity.builder().wsToken("ws_abc").build();

        when(springDataUserSubscriptionRepository.findFirstByUserIdAndStatusOrderByCreatedAtAsc("usr_1", SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(entity));

        Optional<String> result = adapter.findAnyActiveWsTokenByUserId("usr_1");

        assertThat(result).contains("ws_abc");
    }
}
