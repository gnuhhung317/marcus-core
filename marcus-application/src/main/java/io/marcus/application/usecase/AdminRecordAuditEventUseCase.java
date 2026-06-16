package io.marcus.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.domain.model.AdminAuditEvent;
import io.marcus.domain.port.AdminAuditEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminRecordAuditEventUseCase {

    private final AdminAuditEventPort adminAuditEventPort;
    private final ObjectMapper objectMapper;

    public AdminAuditEvent execute(
            String actorUserId,
            String action,
            String targetType,
            String targetId,
            String reason,
            Object beforeState,
            Object afterState
    ) {
        AdminAuditEvent event = AdminAuditEvent.builder()
                .adminAuditEventId("audit_" + UUID.randomUUID().toString().replace("-", ""))
                .actorUserId(actorUserId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason)
                .beforeStateJson(toJson(beforeState))
                .afterStateJson(toJson(afterState))
                .createdAt(LocalDateTime.now())
                .build();

        return adminAuditEventPort.save(event);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize admin audit payload", e);
        }
    }
}
