package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.User;
import io.marcus.domain.port.UserRegistrationPort;
import io.marcus.infrastructure.persistence.entity.UserEntity;
import io.marcus.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class JpaUserRegistrationAdapter implements UserRegistrationPort {

    private final SpringDataUserRepository springDataUserRepository;
    private final UserMapper userMapper;
    private final UserPortfolioProvisioningService userPortfolioProvisioningService;

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        springDataUserRepository.findByUserId(user.getUserId())
                .ifPresent(existing -> entity.setId(existing.getId()));
        User savedUser = userMapper.toDomain(springDataUserRepository.save(entity));
        userPortfolioProvisioningService.ensurePortfolioExists(savedUser.getUserId());
        return savedUser;
    }
}
