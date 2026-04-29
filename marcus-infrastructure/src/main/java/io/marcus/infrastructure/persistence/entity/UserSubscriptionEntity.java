package io.marcus.infrastructure.persistence.entity;

import io.marcus.domain.vo.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "subscriptions",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_subscriptions_user_bot_status", columnNames = {"user_id", "bot_id", "status"}),
            @UniqueConstraint(name = "uk_subscriptions_user_subscription_id", columnNames = {"user_subscription_id"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserSubscriptionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_subscription_id", nullable = false)
    private String userSubscriptionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "bot_id", nullable = false)
    private String botId;

    @Column(name = "package_id")
    private String packageId;

    @Column(name = "ws_token", nullable = false)
    private String wsToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "executor_connected", nullable = false)
    private boolean executorConnected = false;
}
