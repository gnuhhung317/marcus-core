package io.marcus.domain.service;

import io.marcus.domain.vo.Role;
import java.util.Optional;

public interface IdentityService {

    Optional<String> getCurrentUserId();

    Optional<Role> getCurrentUserRole();
}
