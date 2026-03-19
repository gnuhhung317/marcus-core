package io.marcus.domain.repository;

import io.marcus.domain.vo.Role;

public interface UserRepository {

    boolean existsById(String id);
    boolean existsByIdAndRole(String id, Role role);
}
