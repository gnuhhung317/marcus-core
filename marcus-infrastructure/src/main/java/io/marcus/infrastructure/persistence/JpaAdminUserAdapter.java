package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.User;
import io.marcus.domain.model.PagedResult;
import io.marcus.domain.port.AdminUserPort;
import io.marcus.domain.vo.Role;
import io.marcus.infrastructure.persistence.entity.UserEntity;
import io.marcus.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaAdminUserAdapter implements AdminUserPort {

    private final SpringDataUserRepository springDataUserRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public PagedResult<User> search(String query, Role role, Boolean banned, int page, int size) {
        Page<UserEntity> result = springDataUserRepository.searchAdminUsers(
                normalize(query),
                role,
                banned,
                PageRequest.of(Math.max(0, page), Math.max(1, size))
        );
        return new PagedResult<>(
                result.getContent().stream().map(userMapper::toDomain).toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }
        return springDataUserRepository.findByUserId(userId.trim()).map(userMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findByUserIds(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return springDataUserRepository.findByUserIdIn(userIds).stream().map(userMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = userMapper.toEntity(user);
        springDataUserRepository.findByUserId(user.getUserId())
                .ifPresent(existing -> entity.setId(existing.getId()));
        return userMapper.toDomain(springDataUserRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public long countAll() {
        return springDataUserRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByBannedTrue() {
        return springDataUserRepository.countByBannedTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public long countByRoleAndBannedFalse(Role role) {
        return springDataUserRepository.countByRoleAndBannedFalse(role);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
