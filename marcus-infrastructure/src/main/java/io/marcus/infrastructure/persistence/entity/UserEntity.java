package io.marcus.infrastructure.persistence.entity;

import io.marcus.domain.vo.Role;
import io.marcus.infrastructure.persistence.converter.RoleAttributeConverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class UserEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String userId;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true)
    private String email;

    @Convert(converter = RoleAttributeConverter.class)
    @Column(nullable = false)
    private Role role;

    @Column(name = "is_banned", nullable = false)
    private boolean banned = false;

    @Column(name = "banned_at")
    private java.time.LocalDateTime bannedAt;

    @Column(name = "banned_by_user_id")
    private String bannedByUserId;

    @Column(name = "ban_reason")
    private String banReason;
}
