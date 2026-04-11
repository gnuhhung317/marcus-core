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
        if (registerUserRequest.username() == null || registerUserRequest.username().isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (registerUserRequest.password() == null || registerUserRequest.password().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (registerUserRequest.email() == null || registerUserRequest.email().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (userUniquenessPort.existsByUsername(registerUserRequest.username())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userUniquenessPort.existsByEmail(registerUserRequest.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .userId("usr_" + UUID.randomUUID().toString().replace("-", ""))
                .username(registerUserRequest.username())
                .passwordHash(passwordHashPort.encode(registerUserRequest.password()))
                .email(registerUserRequest.email())
                .role(Role.USER)
                .build();

        User savedUser = userRegistrationPort.save(user);

        return new RegisterUserResponse(
                savedUser.getUserId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }
}
