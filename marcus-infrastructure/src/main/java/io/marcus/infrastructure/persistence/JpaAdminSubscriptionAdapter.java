package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.model.PagedResult;
import io.marcus.domain.port.AdminSubscriptionPort;
import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import io.marcus.infrastructure.persistence.mapper.UserSubscriptionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaAdminSubscriptionAdapter implements AdminSubscriptionPort {

    private final SpringDataUserSubscriptionRepository springDataUserSubscriptionRepository;
    private final UserSubscriptionMapper userSubscriptionMapper;

    @Override
    @Transactional(readOnly = true)
    public PagedResult<UserSubscription> searchByBotId(String botId, SubscriptionStatus status, int page, int size) {
        Page<UserSubscriptionEntity> result = springDataUserSubscriptionRepository.searchAdminSubscriptions(
                normalize(botId),
                status,
                PageRequest.of(Math.max(0, page), Math.max(1, size))
        );
        return new PagedResult<>(
                result.getContent().stream().map(userSubscriptionMapper::toDomain).toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSubscription> findByBotIdAndStatus(String botId, SubscriptionStatus status) {
        if (botId == null || botId.isBlank() || status == null) {
            return List.of();
        }
        return springDataUserSubscriptionRepository.findByBotIdAndStatusOrderByCreatedAtDesc(botId.trim(), status)
                .stream()
                .map(userSubscriptionMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSubscription> findByUserSubscriptionId(String userSubscriptionId) {
        if (userSubscriptionId == null || userSubscriptionId.isBlank()) {
            return Optional.empty();
        }
        return springDataUserSubscriptionRepository.findFirstByUserSubscriptionId(userSubscriptionId.trim())
                .map(userSubscriptionMapper::toDomain);
    }

    @Override
    @Transactional
    public UserSubscription save(UserSubscription subscription) {
        UserSubscriptionEntity entity = userSubscriptionMapper.toEntity(subscription);
        springDataUserSubscriptionRepository.findFirstByUserSubscriptionId(subscription.getUserSubscriptionId())
                .ifPresent(existing -> entity.setId(existing.getId()));
        return userSubscriptionMapper.toDomain(springDataUserSubscriptionRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public long countActive() {
        return springDataUserSubscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public long countDisconnectedActiveExecutors() {
        return springDataUserSubscriptionRepository.countByStatusAndExecutorConnectedFalse(SubscriptionStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByBotId(String botId) {
        if (botId == null || botId.isBlank()) {
            return 0L;
        }
        return springDataUserSubscriptionRepository.countByBotId(botId.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByBotIdAndStatus(String botId, SubscriptionStatus status) {
        if (botId == null || botId.isBlank() || status == null) {
            return 0L;
        }
        return springDataUserSubscriptionRepository.countByBotIdAndStatus(botId.trim(), status);
    }

    @Override
    @Transactional
    public void forceCancel(String userSubscriptionId, String canceledByAdminId, String cancellationReason) {
        springDataUserSubscriptionRepository.findFirstByUserSubscriptionId(userSubscriptionId)
                .ifPresent(entity -> {
                    entity.setStatus(SubscriptionStatus.CANCELED);
                    entity.setEndDate(LocalDateTime.now());
                    entity.setExecutorConnected(false);
                    entity.setCanceledByAdminId(canceledByAdminId);
                    entity.setCancellationReason(cancellationReason);
                    entity.setCanceledAt(LocalDateTime.now());
                    springDataUserSubscriptionRepository.save(entity);
                });
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
