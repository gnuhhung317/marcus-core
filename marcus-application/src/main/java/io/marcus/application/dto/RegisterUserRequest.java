package io.marcus.application.dto;

import io.marcus.domain.vo.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterUserRequest(
        @NotBlank(message = "Username is required")
        String username,
        
        String displayName,
        
        @NotBlank(message = "Password is required")
        String password,
        
        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        String email,
        
        Role role
) {}
