package io.marcus.infrastructure.persistence;

import io.marcus.domain.vo.Role;
import io.marcus.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, String> {

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserEntity u WHERE u.userId = :id AND u.role = :role")
    boolean existsByIdAndRole(String id, Role role);

    Optional<UserEntity> findByUserId(String userId);

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByUserId(String userId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
