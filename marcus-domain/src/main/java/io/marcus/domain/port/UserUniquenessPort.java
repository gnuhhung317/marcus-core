package io.marcus.domain.port;

public interface UserUniquenessPort {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
