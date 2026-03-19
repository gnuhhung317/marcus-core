package io.marcus.domain.port;

import io.marcus.domain.vo.AuthenticatedUser;

import java.util.Optional;

public interface AccessTokenPort {

    String issueToken(AuthenticatedUser authenticatedUser);

    Optional<AuthenticatedUser> parseToken(String token);

    long accessTokenTtlSeconds();
}
