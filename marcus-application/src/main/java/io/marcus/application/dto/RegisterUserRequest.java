package io.marcus.application.dto;

import io.marcus.domain.vo.Role;

public record RegisterUserRequest(
        String username,
        String displayName,
        String password,
        String email,
        Role role
        ) {

}
