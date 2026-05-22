package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.SubscriptionPlan;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.SubscriptionReadPort;
import io.marcus.infrastructure.persistence.jpa.SubscriptionJpaRepository;
import io.marcus.infrastructure.persistence.jpa.JpaSubscriptionPlanEntity;
import io.marcus.infrastructure.persistence.jpa.UserSubscriptionJpaRepository;
import io.marcus.infrastructure.persistence.jpa.AuditJpaRepository;
import io.marcus.infrastructure.persistence.jpa.JpaAuditRecordEntity;
import io.marcus.infrastructure.persistence.jpa.JpaUserSubscriptionEntity;
import io.marcus.domain.port.UserSubscriptionPersistencePort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SubscriptionPersistenceAdapter implements SubscriptionReadPort, io.marcus.domain.port.SubscriptionWritePort {

    private final SubscriptionJpaRepository repo;
    private final UserSubscriptionJpaRepository userRepo;
    private final AuditJpaRepository auditRepo;
    private final UserSubscriptionPersistencePort userSubscriptionPersistencePort;

    public SubscriptionPersistenceAdapter(SubscriptionJpaRepository repo, UserSubscriptionJpaRepository userRepo, AuditJpaRepository auditRepo, UserSubscriptionPersistencePort userSubscriptionPersistencePort){
        this.repo = repo;
        this.userRepo = userRepo;
        this.auditRepo = auditRepo;
        this.userSubscriptionPersistencePort = userSubscriptionPersistencePort;
    }

    @Override
    public List<SubscriptionPlan> findByBotId(String botId) {
        return repo.findByBotId(botId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public SubscriptionPlan findById(String planId) {
        return repo.findById(planId).map(this::toDomain).orElse(null);
    }

    private SubscriptionPlan toDomain(JpaSubscriptionPlanEntity e){
        SubscriptionPlan p = new SubscriptionPlan();
        p.id = e.getId();
        p.botId = e.getBotId();
        try{
            p.paymentModel = SubscriptionPlan.PaymentModel.valueOf(e.getPaymentModel());
        }catch(Exception ex){
            p.paymentModel = null;
        }
        // store raw JSON blobs in string fields on domain for now
        p.tiers = null;
        p.tiersJson = e.getTiersJson();
        p.trialDays = e.getTrialDays();
        try{
            p.status = SubscriptionPlan.Status.valueOf(e.getStatus());
        }catch(Exception ex){
            p.status = null;
        }
        return p;
    }

    // write methods
    @Override
    public SubscriptionPlan savePlan(SubscriptionPlan plan){
        JpaSubscriptionPlanEntity e = new JpaSubscriptionPlanEntity();
        e.setId(plan.id == null ? java.util.UUID.randomUUID().toString() : plan.id);
        e.setBotId(plan.botId);
        e.setPaymentModel(plan.paymentModel == null ? null : plan.paymentModel.name());
        e.setTiersJson(plan.tiersJson);
        e.setTrialDays(plan.trialDays);
        e.setStatus(plan.status == null ? null : plan.status.name());
        e.setCreatedAt(plan.createdAt == null ? java.time.Instant.now() : plan.createdAt);
        e.setUpdatedAt(java.time.Instant.now());
        JpaSubscriptionPlanEntity saved = repo.save(e);
        // write audit
        try{
            JpaAuditRecordEntity a = new JpaAuditRecordEntity();
            a.setId(java.util.UUID.randomUUID().toString());
            a.setResourceType("subscription_plan");
            a.setResourceId(saved.getId());
            a.setAction("create_or_update");
            a.setAfterJson(saved.getTiersJson());
            a.setTimestamp(java.time.Instant.now());
            auditRepo.save(a);
        }catch(Exception ex){
            // swallow audit errors to avoid breaking main flow
        }
        return toDomain(saved);
    }

    @Override
    public UserSubscription createUserSubscription(UserSubscription sub){
        // delegate to existing user subscription persistence adapter
        UserSubscription saved = userSubscriptionPersistencePort.save(sub);
        try{
            JpaAuditRecordEntity a = new JpaAuditRecordEntity();
            a.setId(java.util.UUID.randomUUID().toString());
            a.setResourceType("user_subscription");
            a.setResourceId(saved.getUserSubscriptionId());
            a.setAction("create");
            a.setAfterJson("{\"packageId\":\""+saved.getPackageId()+"\",\"userId\":\""+saved.getUserId()+"\"}");
            a.setTimestamp(java.time.Instant.now());
            auditRepo.save(a);
        }catch(Exception ex){ }
        return saved;
    }
}
