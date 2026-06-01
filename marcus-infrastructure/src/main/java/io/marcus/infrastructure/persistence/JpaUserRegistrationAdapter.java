package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.User;
import io.marcus.domain.port.UserRegistrationPort;
import io.marcus.infrastructure.persistence.entity.UserEntity;
import io.marcus.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaUserRegistrationAdapter implements UserRegistrationPort {

    private final SpringDataUserRepository springDataUserRepository;
    private final UserMapper userMapper;

    @Override
    public User save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        springDataUserRepository.findByUserId(user.getUserId())
                .ifPresent(existing -> entity.setId(existing.getId()));
        return userMapper.toDomain(springDataUserRepository.save(entity));
    }
}
