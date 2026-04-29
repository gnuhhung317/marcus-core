package io.marcus.domain.port;

import io.marcus.domain.model.User;

import java.util.Optional;

public interface UserCredentialQueryPort {

    Optional<User> findByUserId(String userId);

    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameOrEmail(String loginIdentifier);
}
