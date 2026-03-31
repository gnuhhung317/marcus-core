package io.marcus.application.usecase;

import io.marcus.application.dto.LoginRequest;
import io.marcus.application.dto.LoginResponse;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.model.User;
import io.marcus.domain.port.AccessTokenPort;
import io.marcus.domain.port.PasswordHashPort;
import io.marcus.domain.port.RefreshTokenPort;
import io.marcus.domain.port.UserCredentialQueryPort;
import io.marcus.domain.vo.AuthenticatedUser;
import org.springframework.stereotype.Service;

@Service
public class AuthenticateUserUseCase {

    private final UserCredentialQueryPort userCredentialQueryPort;
    private final PasswordHashPort passwordHashPort;
    private final AccessTokenPort accessTokenPort;
    private final RefreshTokenPort refreshTokenPort;

    public AuthenticateUserUseCase(UserCredentialQueryPort userCredentialQueryPort,
            PasswordHashPort passwordHashPort,
            AccessTokenPort accessTokenPort,
            RefreshTokenPort refreshTokenPort) {
        this.userCredentialQueryPort = userCredentialQueryPort;
        this.passwordHashPort = passwordHashPort;
        this.accessTokenPort = accessTokenPort;
        this.refreshTokenPort = refreshTokenPort;
    }

    public LoginResponse execute(LoginRequest loginRequest) {
        User user = userCredentialQueryPort.findByUsername(loginRequest.username())
                .orElseThrow(() -> new UnauthenticatedException("Invalid username or password"));

        if (!passwordHashPort.matches(loginRequest.password(), user.getPasswordHash())) {
            throw new UnauthenticatedException("Invalid username or password");
        }

        AuthenticatedUser authenticatedUser = new AuthenticatedUser(user.getUserId(), user.getUsername(), user.getRole());
        String accessToken = accessTokenPort.issueToken(authenticatedUser);
        String refreshToken = refreshTokenPort.issueRefreshToken(authenticatedUser);

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                accessTokenPort.accessTokenTtlSeconds(),
                refreshTokenPort.refreshTokenTtlSeconds(),
                user.getUserId(),
                user.getUsername(),
                user.getRole().name()
        );
    }
}
