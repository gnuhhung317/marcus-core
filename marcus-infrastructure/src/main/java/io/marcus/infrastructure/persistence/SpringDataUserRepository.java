package io.marcus.infrastructure.persistence;

import io.marcus.domain.vo.Role;
import io.marcus.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SpringDataUserRepository extends JpaRepository<UserEntity, String> {

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserEntity u WHERE u.userId = :id AND u.role = :role")
    boolean existsByIdAndRole(String id, Role role);

    Optional<UserEntity> findByUserId(String userId);

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByUsernameIgnoreCase(String username);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    List<UserEntity> findByUserIdIn(Collection<String> userIds);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByBannedTrue();

    long countByRoleAndBannedFalse(Role role);

    @Query("""
        SELECT u FROM UserEntity u
        WHERE (:query IS NULL OR :query = '' OR
               LOWER(u.userId) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR
               LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:role IS NULL OR u.role = :role)
          AND (:banned IS NULL OR u.banned = :banned)
        ORDER BY u.createdAt DESC
    """)
    Page<UserEntity> searchAdminUsers(
            @Param("query") String query,
            @Param("role") Role role,
            @Param("banned") Boolean banned,
            Pageable pageable
    );
}
