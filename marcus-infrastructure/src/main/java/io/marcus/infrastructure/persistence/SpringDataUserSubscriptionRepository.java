package io.marcus.infrastructure.persistence;

import io.marcus.domain.vo.SubscriptionStatus;
import io.marcus.infrastructure.persistence.entity.UserSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataUserSubscriptionRepository extends JpaRepository<UserSubscriptionEntity, String> {

    List<UserSubscriptionEntity> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, SubscriptionStatus status);

    Optional<UserSubscriptionEntity> findFirstByUserIdAndBotIdAndStatusOrderByCreatedAtDesc(
            String userId,
            String botId,
            SubscriptionStatus status
    );

    Optional<UserSubscriptionEntity> findByUserIdAndBotIdAndStatus(String userId, String botId, SubscriptionStatus status);

    Optional<UserSubscriptionEntity> findFirstByUserIdAndStatusOrderByCreatedAtAsc(String userId, SubscriptionStatus status);

    List<UserSubscriptionEntity> findByBotIdAndStatusOrderByCreatedAtDesc(String botId, SubscriptionStatus status);

    List<UserSubscriptionEntity> findByBotIdOrderByCreatedAtDesc(String botId);

    Optional<UserSubscriptionEntity> findFirstByUserSubscriptionId(String userSubscriptionId);

    long countByStatus(SubscriptionStatus status);

    long countByStatusAndExecutorConnectedFalse(SubscriptionStatus status);

    long countByBotId(String botId);

    long countByBotIdAndStatus(String botId, SubscriptionStatus status);

    @Query("""
        SELECT s FROM UserSubscriptionEntity s
        WHERE (:botId IS NULL OR :botId = '' OR s.botId = :botId)
          AND (:status IS NULL OR s.status = :status)
        ORDER BY s.createdAt DESC
    """)
    Page<UserSubscriptionEntity> searchAdminSubscriptions(
            @Param("botId") String botId,
            @Param("status") SubscriptionStatus status,
            Pageable pageable
    );

    // Pha 1: Decision Dashboard queries
    /**
     * Find all subscriptions for given user with optional status filter.
     * Returns subscriptions sorted by creation date descending.
     *
     * @param userId user identifier
     * @param status filter by status, or null for all subscriptions
     * @return list of user subscriptions
     */
    @Query("""
        SELECT s FROM UserSubscriptionEntity s
        WHERE s.userId = :userId AND (:status IS NULL OR s.status = :status)
        ORDER BY s.createdAt DESC
    """)
    List<UserSubscriptionEntity> findByUserIdAndStatus(
            @Param("userId") String userId,
            @Param("status") SubscriptionStatus status
    );

    /**
     * Count active (non-paused) subscriptions for user. Used in portfolio
     * overview.
     *
     * @param userId user identifier
     * @param status filter by status
     * @return count of subscriptions with given status
     */
    long countByUserIdAndStatus(String userId, SubscriptionStatus status);

    // --- Aggregate queries (replacing findAll().stream().groupingBy patterns) ---

    /**
     * Count subscriptions per bot grouped by status. Replaces
     * {@code findAll().stream().filter(ACTIVE).collect(groupingBy(botId, counting()))}.
     *
     * @param status subscription status filter
     * @return list of [botId, count] tuples
     */
    @Query("SELECT s.botId, COUNT(s) FROM UserSubscriptionEntity s " +
           "WHERE s.status = :status GROUP BY s.botId")
    List<Object[]> countByStatusGroupByBotId(@Param("status") SubscriptionStatus status);
}
