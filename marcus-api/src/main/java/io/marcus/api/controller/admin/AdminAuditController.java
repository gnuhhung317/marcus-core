package io.marcus.api.controller.admin;

import io.marcus.application.dto.AdminDtos;
import io.marcus.domain.port.AdminAuditEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin", "/api/admin", "/api/v1/admin"})
@RequiredArgsConstructor
public class AdminAuditController {

    private final AdminAuditEventPort adminAuditEventPort;

    @GetMapping("/audit-events")
    public ResponseEntity<AdminDtos.PageResponse<AdminDtos.AuditEventRow>> listAuditEvents(
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String actorUserId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, 100));
        var result = adminAuditEventPort.search(targetType, targetId, actorUserId, action, normalizedPage, normalizedSize);

        return ResponseEntity.ok(new AdminDtos.PageResponse<>(
                result.getContent().stream().map(event -> new AdminDtos.AuditEventRow(
                        event.getAdminAuditEventId(),
                        event.getActorUserId(),
                        event.getAction(),
                        event.getTargetType(),
                        event.getTargetId(),
                        event.getReason(),
                        event.getBeforeStateJson(),
                        event.getAfterStateJson(),
                        event.getCreatedAt()
                )).toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize(),
                result.hasNext()
        ));
    }
}
