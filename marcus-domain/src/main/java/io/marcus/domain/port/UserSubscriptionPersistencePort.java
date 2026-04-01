package io.marcus.domain.port;

import io.marcus.domain.model.UserSubscription;

import java.util.List;
import java.util.Optional;

public interface UserSubscriptionPersistencePort {

    UserSubscription save(UserSubscription userSubscription);

    Optional<UserSubscription> findActiveByUserIdAndBotId(String userId, String botId);

    List<UserSubscription> findActiveByUserId(String userId);

    Optional<String> findAnyActiveWsTokenByUserId(String userId);
}
