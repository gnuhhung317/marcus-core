package io.marcus.domain.port;

import io.marcus.domain.model.AdminAuditEvent;
import io.marcus.domain.model.PagedResult;

public interface AdminAuditEventPort {
    AdminAuditEvent save(AdminAuditEvent event);

    PagedResult<AdminAuditEvent> search(String targetType, String targetId, String actorUserId, String action, int page, int size);
}
