package io.marcus.domain.port;

import io.marcus.domain.model.UserSubscription;

import java.util.List;
import java.util.Optional;

public interface UserSubscriptionPersistencePort {

    List<UserSubscription> findActiveByUserId(String userId);

    Optional<UserSubscription> findActiveByUserIdAndBotId(String userId, String botId);

    UserSubscription save(UserSubscription subscription);

    void cancelActiveByUserIdAndBotId(String userId, String botId);

    List<UserSubscription> findActiveByBotId(String botId);

    Optional<UserSubscription> findActiveByBotIdAndWsToken(String botId, String wsToken);

    void markExecutorConnected(String userSubscriptionId, boolean connected);
}
