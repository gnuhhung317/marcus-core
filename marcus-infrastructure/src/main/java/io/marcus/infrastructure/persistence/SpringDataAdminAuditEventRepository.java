package io.marcus.infrastructure.persistence;

import io.marcus.infrastructure.persistence.entity.AdminAuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpringDataAdminAuditEventRepository extends JpaRepository<AdminAuditEventEntity, String> {

    Optional<AdminAuditEventEntity> findByAdminAuditEventId(String adminAuditEventId);

    @Query("""
        SELECT e FROM AdminAuditEventEntity e
        WHERE (:targetType IS NULL OR e.targetType = :targetType)
          AND (:targetId IS NULL OR e.targetId = :targetId)
          AND (:actorUserId IS NULL OR e.actorUserId = :actorUserId)
          AND (:action IS NULL OR e.action = :action)
        ORDER BY e.createdAt DESC
    """)
    Page<AdminAuditEventEntity> search(
            @Param("targetType") String targetType,
            @Param("targetId") String targetId,
            @Param("actorUserId") String actorUserId,
            @Param("action") String action,
            Pageable pageable
    );
}
