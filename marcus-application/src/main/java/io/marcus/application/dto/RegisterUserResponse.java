package io.marcus.application.dto;

public record RegisterUserResponse(
        String userId,
        String username,
        String email,
        String role
        ) {

}
