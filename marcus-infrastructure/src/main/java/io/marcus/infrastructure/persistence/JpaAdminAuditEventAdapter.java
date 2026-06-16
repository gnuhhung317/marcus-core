package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.AdminAuditEvent;
import io.marcus.domain.model.PagedResult;
import io.marcus.domain.port.AdminAuditEventPort;
import io.marcus.infrastructure.persistence.entity.AdminAuditEventEntity;
import io.marcus.infrastructure.persistence.mapper.AdminAuditEventMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class JpaAdminAuditEventAdapter implements AdminAuditEventPort {

    private final SpringDataAdminAuditEventRepository springDataAdminAuditEventRepository;
    private final AdminAuditEventMapper adminAuditEventMapper;

    @Override
    @Transactional
    public AdminAuditEvent save(AdminAuditEvent event) {
        AdminAuditEventEntity entity = adminAuditEventMapper.toEntity(event);
        springDataAdminAuditEventRepository.findByAdminAuditEventId(event.getAdminAuditEventId())
                .ifPresent(existing -> entity.setId(existing.getId()));
        return adminAuditEventMapper.toDomain(springDataAdminAuditEventRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<AdminAuditEvent> search(String targetType, String targetId, String actorUserId, String action, int page, int size) {
        Page<AdminAuditEventEntity> result = springDataAdminAuditEventRepository.search(
                normalize(targetType),
                normalize(targetId),
                normalize(actorUserId),
                normalize(action),
                PageRequest.of(Math.max(0, page), Math.max(1, size))
        );
        return new PagedResult<>(
                result.getContent().stream().map(adminAuditEventMapper::toDomain).toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize()
        );
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
