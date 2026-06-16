package io.marcus.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "admin_audit_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AdminAuditEventEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "admin_audit_event_id", nullable = false, unique = true)
    private String adminAuditEventId;

    @Column(name = "actor_user_id", nullable = false)
    private String actorUserId;

    @Column(nullable = false)
    private String action;

    @Column(name = "target_type", nullable = false)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @Column(columnDefinition = "text")
    private String reason;

    @Column(name = "before_state_json", columnDefinition = "text")
    private String beforeStateJson;

    @Column(name = "after_state_json", columnDefinition = "text")
    private String afterStateJson;
}
