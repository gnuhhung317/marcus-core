package io.marcus.application.dto;

public record UpdateUserPreferencesRequest(
        String timezone,
        String locale,
        Boolean emailNotificationsEnabled
) {
}
