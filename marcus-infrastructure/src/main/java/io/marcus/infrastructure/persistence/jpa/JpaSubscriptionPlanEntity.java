package io.marcus.infrastructure.persistence.jpa;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "subscription_plan")
public class JpaSubscriptionPlanEntity {
    @Id
    private String id;

    @Column(name = "bot_id")
    private String botId;

    @Column(name = "payment_model")
    private String paymentModel;

    @Column(name = "tiers_json", columnDefinition = "text")
    private String tiersJson;

    @Column(name = "cycle_months_json", columnDefinition = "text")
    private String cycleMonthsJson;

    @Column(name = "discount_rules_json", columnDefinition = "text")
    private String discountRulesJson;

    @Column(name = "trial_days")
    private Integer trialDays;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBotId() {
        return botId;
    }

    public void setBotId(String botId) {
        this.botId = botId;
    }

    public String getPaymentModel() {
        return paymentModel;
    }

    public void setPaymentModel(String paymentModel) {
        this.paymentModel = paymentModel;
    }

    public String getTiersJson() {
        return tiersJson;
    }

    public void setTiersJson(String tiersJson) {
        this.tiersJson = tiersJson;
    }

    public String getCycleMonthsJson() {
        return cycleMonthsJson;
    }

    public void setCycleMonthsJson(String cycleMonthsJson) {
        this.cycleMonthsJson = cycleMonthsJson;
    }

    public String getDiscountRulesJson() {
        return discountRulesJson;
    }

    public void setDiscountRulesJson(String discountRulesJson) {
        this.discountRulesJson = discountRulesJson;
    }

    public Integer getTrialDays() {
        return trialDays;
    }

    public void setTrialDays(Integer trialDays) {
        this.trialDays = trialDays;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
