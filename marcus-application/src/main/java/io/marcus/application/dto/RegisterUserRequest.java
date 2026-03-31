package io.marcus.application.dto;

public record RegisterUserRequest(
        String username,
        String password,
        String email
        ) {

}
