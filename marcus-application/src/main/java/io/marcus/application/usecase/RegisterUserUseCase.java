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
        String normalizedUsername = registerUserRequest.username().trim();
        String normalizedEmail = registerUserRequest.email().trim();
        String rawPassword = registerUserRequest.password();
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

    private Role resolveRequestedRole(Role requestedRole) {
        if (requestedRole == null) {
            return Role.TRADER;
        }

        if (requestedRole == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot request ADMIN role during public registration");
        }

        return requestedRole;
    }
}
