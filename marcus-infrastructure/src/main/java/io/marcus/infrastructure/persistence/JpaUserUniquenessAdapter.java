package io.marcus.infrastructure.persistence;

import io.marcus.domain.port.UserUniquenessPort;
import org.springframework.stereotype.Component;

@Component
public class JpaUserUniquenessAdapter implements UserUniquenessPort {

    private final SpringDataUserRepository springDataUserRepository;

    public JpaUserUniquenessAdapter(SpringDataUserRepository springDataUserRepository) {
        this.springDataUserRepository = springDataUserRepository;
    }

    @Override
    public boolean existsByUsername(String username) {
        return springDataUserRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataUserRepository.existsByEmail(email);
    }
}
