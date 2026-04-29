package io.marcus.application.dto;

public record RegisterUserRequest(
        String username,
        String displayName,
        String password,
        String email
        ) {

}
