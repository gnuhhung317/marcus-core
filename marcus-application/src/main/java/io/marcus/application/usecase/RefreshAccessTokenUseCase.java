package io.marcus.application.usecase;

import io.marcus.application.dto.LoginResponse;
import io.marcus.application.dto.RefreshTokenRequest;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.model.User;
import io.marcus.domain.port.AccessTokenPort;
import io.marcus.domain.port.RefreshTokenPort;
import io.marcus.domain.port.UserCredentialQueryPort;
import io.marcus.domain.vo.AuthenticatedUser;
import org.springframework.stereotype.Service;

@Service
public class RefreshAccessTokenUseCase {

    private final RefreshTokenPort refreshTokenPort;
    private final AccessTokenPort accessTokenPort;
    private final UserCredentialQueryPort userCredentialQueryPort;

    public RefreshAccessTokenUseCase(RefreshTokenPort refreshTokenPort,
                                     AccessTokenPort accessTokenPort,
                                     UserCredentialQueryPort userCredentialQueryPort) {
        this.refreshTokenPort = refreshTokenPort;
        this.accessTokenPort = accessTokenPort;
        this.userCredentialQueryPort = userCredentialQueryPort;
    }

    public LoginResponse execute(RefreshTokenRequest refreshTokenRequest) {
        AuthenticatedUser tokenUser = refreshTokenPort.consumeRefreshToken(refreshTokenRequest.refreshToken())
                .orElseThrow(() -> new UnauthenticatedException("Invalid refresh token"));

        User user = userCredentialQueryPort.findByUsername(tokenUser.username())
                .orElseThrow(() -> new UnauthenticatedException("Invalid refresh token"));

        if (!user.getUserId().equals(tokenUser.userId())) {
            throw new UnauthenticatedException("Invalid refresh token");
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
