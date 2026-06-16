package io.marcus.domain.port;

import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.model.PagedResult;
import io.marcus.domain.vo.SubscriptionStatus;

import java.util.List;
import java.util.Optional;

public interface AdminSubscriptionPort {
    PagedResult<UserSubscription> searchByBotId(String botId, SubscriptionStatus status, int page, int size);

    List<UserSubscription> findByBotIdAndStatus(String botId, SubscriptionStatus status);

    Optional<UserSubscription> findByUserSubscriptionId(String userSubscriptionId);

    UserSubscription save(UserSubscription subscription);

    long countActive();

    long countDisconnectedActiveExecutors();

    long countByBotId(String botId);

    long countByBotIdAndStatus(String botId, SubscriptionStatus status);

    void forceCancel(String userSubscriptionId, String canceledByAdminId, String cancellationReason);
}
