package io.marcus.domain.model;

import io.marcus.domain.vo.SubscriptionStatus;

import java.time.LocalDateTime;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)

public class UserSubscription extends BaseModel{
    private String userSubscriptionId;
    private String botId;
    private String userId;
    private String packageId;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}
