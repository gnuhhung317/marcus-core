package io.marcus.domain.model;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)

public class SubscriptionPackage extends BaseModel{
    private String subscriptionPackageId;
    private String botId;
    private String description;
    private float price;
    private String currency;
    private boolean isActive;
}
