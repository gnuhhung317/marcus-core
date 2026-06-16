package io.marcus.infrastructure.persistence.mapper;

import io.marcus.domain.model.AdminAuditEvent;
import io.marcus.infrastructure.persistence.entity.AdminAuditEventEntity;
import org.springframework.stereotype.Component;

@Component
public class AdminAuditEventMapper {

    public AdminAuditEvent toDomain(AdminAuditEventEntity entity) {
        if (entity == null) {
            return null;
        }

        return AdminAuditEvent.builder()
                .adminAuditEventId(entity.getAdminAuditEventId())
                .actorUserId(entity.getActorUserId())
                .action(entity.getAction())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .reason(entity.getReason())
                .beforeStateJson(entity.getBeforeStateJson())
                .afterStateJson(entity.getAfterStateJson())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }

    public AdminAuditEventEntity toEntity(AdminAuditEvent domain) {
        if (domain == null) {
            return null;
        }

        return AdminAuditEventEntity.builder()
                .adminAuditEventId(domain.getAdminAuditEventId())
                .actorUserId(domain.getActorUserId())
                .action(domain.getAction())
                .targetType(domain.getTargetType())
                .targetId(domain.getTargetId())
                .reason(domain.getReason())
                .beforeStateJson(domain.getBeforeStateJson())
                .afterStateJson(domain.getAfterStateJson())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }
}
