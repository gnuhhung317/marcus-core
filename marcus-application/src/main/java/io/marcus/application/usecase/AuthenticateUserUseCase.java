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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
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
        String loginIdentifier = normalizeLoginIdentifier(loginRequest.username());

        log.info("Auth login attempt received for identifier='{}'", maskLoginIdentifier(loginIdentifier));

        if (loginIdentifier.isBlank()) {
            log.warn("Auth login rejected because identifier is blank");
            throw new UnauthenticatedException("Invalid username or password");
        }

        if (loginRequest.password() == null || loginRequest.password().isBlank()) {
            log.warn("Auth login rejected for identifier='{}' because password is blank", maskLoginIdentifier(loginIdentifier));
            throw new UnauthenticatedException("Invalid username or password");
        }

        User user = userCredentialQueryPort.findByUsernameOrEmail(loginIdentifier)
                .orElseThrow(() -> {
                    log.warn("Auth login user not found for identifier='{}'", maskLoginIdentifier(loginIdentifier));
                    return new UnauthenticatedException("Invalid username or password");
                });

        log.info("Auth login user resolved: userId='{}', username='{}', email='{}', role='{}'",
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole());

        boolean passwordMatched = passwordHashPort.matches(loginRequest.password(), user.getPasswordHash());
        log.info("Auth password match result for userId='{}': {}", user.getUserId(), passwordMatched);

        if (!passwordMatched) {
            log.warn("Auth login rejected for userId='{}' because password mismatch", user.getUserId());
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

    private String normalizeLoginIdentifier(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private String maskLoginIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "<blank>";
        }

        int atIndex = value.indexOf('@');
        if (atIndex > 1) {
            return value.charAt(0) + "***" + value.substring(atIndex);
        }

        if (value.length() <= 4) {
            return "****";
        }

        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
}
