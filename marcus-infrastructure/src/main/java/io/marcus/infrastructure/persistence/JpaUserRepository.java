package io.marcus.infrastructure.persistence;

import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.vo.Role;
import io.marcus.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaUserRepository implements UserRepository {

    private final SpringDataUserRepository springDataUserRepository;

    @Override
    public boolean existsById(String id) {
        return springDataUserRepository.existsById(id);
    }

    @Override
    public boolean existsByIdAndRole(String id, Role role) {
        if (id == null || id.isBlank() || role == null) {
            return false;
        }

        return springDataUserRepository.findByUserId(id.trim())
                .map(UserEntity::getRole)
                .map(mappedRole -> mappedRole == role)
                .orElse(false);
    }

    @Override
    public long count() {
        return springDataUserRepository.count();
    }
}
