package io.marcus.domain.model;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)

public class SubscriptionPackage extends BaseModel{
    private String subscriptionPackageId;
    private String botId;
    private String description;
    private float price;
    private String currency;
    private boolean isActive;
}
