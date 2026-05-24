package io.marcus.application.dto;

public record UpdateUserProfileRequest(
        String username,
        String email
        ) {

}
