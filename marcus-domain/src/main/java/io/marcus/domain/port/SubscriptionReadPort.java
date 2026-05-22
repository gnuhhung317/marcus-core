package io.marcus.domain.port;

import io.marcus.domain.model.SubscriptionPlan;
import java.util.List;

public interface SubscriptionReadPort {
    List<SubscriptionPlan> findByBotId(String botId);
    SubscriptionPlan findById(String planId);
}
