package io.marcus.application.usecase;

import io.marcus.application.dto.RegisterUserRequest;
import io.marcus.application.dto.RegisterUserResponse;
import io.marcus.domain.model.User;
import io.marcus.domain.port.PasswordHashPort;
import io.marcus.domain.port.UserRegistrationPort;
import io.marcus.domain.port.UserUniquenessPort;
import io.marcus.domain.vo.Role;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RegisterUserUseCase {

    private final UserUniquenessPort userUniquenessPort;
    private final PasswordHashPort passwordHashPort;
    private final UserRegistrationPort userRegistrationPort;

    public RegisterUserUseCase(UserUniquenessPort userUniquenessPort,
            PasswordHashPort passwordHashPort,
            UserRegistrationPort userRegistrationPort) {
        this.userUniquenessPort = userUniquenessPort;
        this.passwordHashPort = passwordHashPort;
        this.userRegistrationPort = userRegistrationPort;
    }

    public RegisterUserResponse execute(RegisterUserRequest registerUserRequest) {
        String normalizedUsername = normalizeUsername(registerUserRequest);
        String normalizedEmail = requireTrimmed(registerUserRequest.email(), "Email is required");
        String rawPassword = requireTrimmed(registerUserRequest.password(), "Password is required");
        Role requestedRole = resolveRequestedRole(registerUserRequest.role());

        if (userUniquenessPort.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userUniquenessPort.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .userId("usr_" + UUID.randomUUID().toString().replace("-", ""))
                .username(normalizedUsername)
                .passwordHash(passwordHashPort.encode(rawPassword))
                .email(normalizedEmail)
                .role(requestedRole)
                .build();

        User savedUser = userRegistrationPort.save(user);

        return new RegisterUserResponse(
                savedUser.getUserId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    private String normalizeUsername(RegisterUserRequest registerUserRequest) {
        String username = firstNonBlank(registerUserRequest.username(), registerUserRequest.displayName());
        if (username == null) {
            throw new IllegalArgumentException("Username is required");
        }
        return username;
    }

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = trimToNull(first);
        if (normalizedFirst != null) {
            return normalizedFirst;
        }
        return trimToNull(second);
    }

    private String requireTrimmed(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Role resolveRequestedRole(Role requestedRole) {
        if (requestedRole == null) {
            return Role.USER;
        }

        if (requestedRole == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot request ADMIN role during public registration");
        }

        return requestedRole;
    }
}
