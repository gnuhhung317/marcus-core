package io.marcus.infrastructure.persistence;


import io.marcus.domain.repository.UserRepository;
import io.marcus.domain.vo.Role;
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
        return springDataUserRepository.existsByIdAndRole(id, role);
    }
}
