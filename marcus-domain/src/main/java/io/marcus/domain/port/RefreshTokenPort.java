package io.marcus.domain.port;

import io.marcus.domain.vo.AuthenticatedUser;

import java.util.Optional;

public interface RefreshTokenPort {

    String issueRefreshToken(AuthenticatedUser authenticatedUser);

    Optional<AuthenticatedUser> consumeRefreshToken(String token);

    long refreshTokenTtlSeconds();
}
