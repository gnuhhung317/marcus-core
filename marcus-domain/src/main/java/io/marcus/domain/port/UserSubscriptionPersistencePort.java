package io.marcus.domain.port;

import io.marcus.domain.model.UserSubscription;

import java.util.List;

public interface UserSubscriptionPersistencePort {

    List<UserSubscription> findActiveByUserId(String userId);
}