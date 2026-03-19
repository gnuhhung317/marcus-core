package io.marcus.infrastructure.persistence;

import io.marcus.domain.vo.Role;
import io.marcus.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, String> {

    boolean existsByIdAndRole(String id, Role role);
}
