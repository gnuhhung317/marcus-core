package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import io.marcus.infrastructure.persistence.mapper.UserSubscriptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaUserSubscriptionPersistenceAdapter implements UserSubscriptionPersistencePort {

    private final SpringDataUserSubscriptionRepository springDataUserSubscriptionRepository;
    private final UserSubscriptionMapper userSubscriptionMapper;

    @Override
    public UserSubscription save(UserSubscription userSubscription) {
        UserSubscriptionEntity entity = userSubscriptionMapper.toEntity(userSubscription);
        UserSubscriptionEntity savedEntity = springDataUserSubscriptionRepository.save(entity);
        return userSubscriptionMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<UserSubscription> findActiveByUserIdAndBotId(String userId, String botId) {
        return springDataUserSubscriptionRepository
                .findByUserIdAndBotIdAndStatus(userId, botId, SubscriptionStatus.ACTIVE)
                .map(userSubscriptionMapper::toDomain);
    }

    @Override
    public List<UserSubscription> findActiveByUserId(String userId) {
        return springDataUserSubscriptionRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, SubscriptionStatus.ACTIVE)
                .stream()
                .map(userSubscriptionMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<String> findAnyActiveWsTokenByUserId(String userId) {
        return springDataUserSubscriptionRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtAsc(userId, SubscriptionStatus.ACTIVE)
                .map(UserSubscriptionEntity::getWsToken)
                .filter(token -> token != null && !token.isBlank());
    }
}
