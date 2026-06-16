package io.marcus.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AdminAuditEvent extends BaseModel {
    private String adminAuditEventId;
    private String actorUserId;
    private String action;
    private String targetType;
    private String targetId;
    private String reason;
    private String beforeStateJson;
    private String afterStateJson;
}
