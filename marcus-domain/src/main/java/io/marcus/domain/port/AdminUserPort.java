package io.marcus.domain.port;

import io.marcus.domain.model.User;
import io.marcus.domain.model.PagedResult;
import io.marcus.domain.vo.Role;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AdminUserPort {
    PagedResult<User> search(String query, Role role, Boolean banned, int page, int size);

    Optional<User> findByUserId(String userId);

    List<User> findByUserIds(Collection<String> userIds);

    User save(User user);

    long countAll();

    long countByBannedTrue();

    long countByRoleAndBannedFalse(Role role);
}
