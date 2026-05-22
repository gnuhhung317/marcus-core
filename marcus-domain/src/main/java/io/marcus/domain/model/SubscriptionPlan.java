package io.marcus.domain.model;

import java.time.Instant;
import java.util.List;

public class SubscriptionPlan {
    public String id;
    public String botId;
    public PaymentModel paymentModel;
    public List<Tier> tiers;
    // raw JSON representation for flexible serialization/storage
    public String tiersJson;
    public List<Integer> cycleMonths;
    public List<DiscountRule> discountRules;
    public Integer trialDays;
    public Status status;
    public Instant createdAt;
    public Instant updatedAt;

    public enum PaymentModel { PROFIT_SHARING, PREPAID_SUBSCRIPTION }
    public enum Status { DRAFT, ACTIVE, INACTIVE }

    public static class Tier {
        public String name;
        public long price;
        public String[] features;
    }

    public static class DiscountRule {
        public int minMonths;
        public int discountPercent;
    }
}
