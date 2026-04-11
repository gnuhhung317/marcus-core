package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.User;
import io.marcus.domain.port.UserCredentialQueryPort;
import io.marcus.infrastructure.persistence.mapper.UserMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaUserCredentialQueryAdapter implements UserCredentialQueryPort {

    private final SpringDataUserRepository springDataUserRepository;
    private final UserMapper userMapper;

    public JpaUserCredentialQueryAdapter(SpringDataUserRepository springDataUserRepository, UserMapper userMapper) {
        this.springDataUserRepository = springDataUserRepository;
        this.userMapper = userMapper;
    }

    @Override
    public Optional<User> findByUserId(String userId) {
        return springDataUserRepository.findByUserId(userId).map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataUserRepository.findByUsername(username).map(userMapper::toDomain);
    }

    @Override
    public Optional<User> findByUserId(String userId) {
        return springDataUserRepository.findByUserId(userId).map(userMapper::toDomain);
    }
}
