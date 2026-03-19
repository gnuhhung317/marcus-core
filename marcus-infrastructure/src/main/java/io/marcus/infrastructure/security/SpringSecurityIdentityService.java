package io.marcus.infrastructure.security;

import io.marcus.domain.service.IdentityService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringSecurityIdentityService  implements IdentityService {
    public Optional<String> getCurrentUserId(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // TODO: add id in principal
        return Optional.empty();
    }
}
