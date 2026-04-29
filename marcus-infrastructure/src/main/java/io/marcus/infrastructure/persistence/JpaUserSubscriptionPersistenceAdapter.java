package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import io.marcus.infrastructure.persistence.mapper.UserSubscriptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaUserSubscriptionPersistenceAdapter implements UserSubscriptionPersistencePort {

    private final SpringDataUserSubscriptionRepository springDataUserSubscriptionRepository;
    private final UserSubscriptionMapper userSubscriptionMapper;

    @Override
    public List<UserSubscription> findActiveByUserId(String userId) {
        return springDataUserSubscriptionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .stream()
                .map(userSubscriptionMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<UserSubscription> findActiveByUserIdAndBotId(String userId, String botId) {
        return springDataUserSubscriptionRepository
                .findFirstByUserIdAndBotIdAndStatusOrderByCreatedAtDesc(userId, botId, SubscriptionStatus.ACTIVE)
                .map(userSubscriptionMapper::toDomain);
    }

    @Override
    public UserSubscription save(UserSubscription subscription) {
        UserSubscriptionEntity entity = userSubscriptionMapper.toEntity(subscription);
        UserSubscriptionEntity savedEntity = springDataUserSubscriptionRepository.save(entity);
        return userSubscriptionMapper.toDomain(savedEntity);
    }

    @Override
    public List<UserSubscription> findActiveByBotId(String botId) {
        return springDataUserSubscriptionRepository
                .findByBotIdAndStatusOrderByCreatedAtDesc(botId, SubscriptionStatus.ACTIVE)
                .stream()
                .map(userSubscriptionMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<UserSubscription> findActiveByBotIdAndWsToken(String botId, String wsToken) {
        return springDataUserSubscriptionRepository
                .findByBotIdAndStatusOrderByCreatedAtDesc(botId, SubscriptionStatus.ACTIVE)
                .stream()
                .filter(e -> e.getWsToken() != null && e.getWsToken().equals(wsToken))
                .findFirst()
                .map(userSubscriptionMapper::toDomain);
    }

    @Override
    @Transactional
    public void cancelActiveByUserIdAndBotId(String userId, String botId) {
        springDataUserSubscriptionRepository
                .findFirstByUserIdAndBotIdAndStatusOrderByCreatedAtDesc(userId, botId, SubscriptionStatus.ACTIVE)
                .ifPresent(entity -> {
                    entity.setStatus(SubscriptionStatus.CANCELED);
                    springDataUserSubscriptionRepository.save(entity);
                });
    }

    @Override
    @Transactional
    public void markExecutorConnected(String userSubscriptionId, boolean connected) {
        springDataUserSubscriptionRepository
                .findFirstByUserSubscriptionId(userSubscriptionId)
                .ifPresent(entity -> {
                    entity.setExecutorConnected(connected);
                    springDataUserSubscriptionRepository.save(entity);
                });
    }
}
