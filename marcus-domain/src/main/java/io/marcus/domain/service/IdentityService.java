package io.marcus.domain.service;

import java.util.Optional;

public interface IdentityService {

    Optional<String> getCurrentUserId();
}
