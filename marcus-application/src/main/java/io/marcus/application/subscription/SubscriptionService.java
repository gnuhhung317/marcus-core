package io.marcus.application.subscription;

import io.marcus.domain.model.SubscriptionPlan;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.SubscriptionReadPort;
import io.marcus.domain.port.SubscriptionWritePort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionService {
    private final SubscriptionReadPort readPort;
    private final SubscriptionWritePort writePort;

    public SubscriptionService(SubscriptionReadPort readPort, SubscriptionWritePort writePort){
        this.readPort = readPort;
        this.writePort = writePort;
    }

    public List<SubscriptionPlan> listPlans(String botId){
        return readPort.findByBotId(botId);
    }

    public SubscriptionPlan createPlan(SubscriptionPlan plan){
        return writePort.savePlan(plan);
    }

    public UserSubscription subscribe(String userId, String planId, String tierName){
        UserSubscription sub = UserSubscription.builder()
            .userId(userId)
            .packageId(planId)
            .startDate(java.time.LocalDateTime.now())
            .endDate(java.time.LocalDateTime.now().plusDays(30))
            .status(null)
            .build();
        return writePort.createUserSubscription(sub);
    }
}
