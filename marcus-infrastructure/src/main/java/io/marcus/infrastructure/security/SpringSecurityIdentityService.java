package io.marcus.infrastructure.security;

import io.marcus.domain.service.IdentityService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringSecurityIdentityService implements IdentityService {

    public Optional<String> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        String userId = authentication.getName();
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(userId);
    }
}
