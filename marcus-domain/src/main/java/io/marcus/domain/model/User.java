package io.marcus.domain.model;

import io.marcus.domain.vo.Role;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class User extends BaseModel{
    private String userId;
    private String username;
    @ToString.Exclude
    private String passwordHash;
    private String email;
    private Role role;
}
