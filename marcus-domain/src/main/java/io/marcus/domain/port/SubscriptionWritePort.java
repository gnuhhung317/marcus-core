package io.marcus.domain.port;

import io.marcus.domain.model.SubscriptionPlan;
import io.marcus.domain.model.UserSubscription;

public interface SubscriptionWritePort {
    SubscriptionPlan savePlan(SubscriptionPlan plan);
    UserSubscription createUserSubscription(UserSubscription sub);
}
