package io.marcus.domain.model;

import java.time.Instant;

public class AuditRecord {
    public String id;
    public String resourceType;
    public String resourceId;
    public String actorId;
    public String actorRole;
    public String action;
    public Instant timestamp;
    public String traceId;
    public String beforeJson;
    public String afterJson;
}
