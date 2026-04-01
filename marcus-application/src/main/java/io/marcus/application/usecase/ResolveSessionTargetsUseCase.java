package io.marcus.application.usecase;

import io.marcus.application.dto.ResolveSessionTargetsRequest;
import io.marcus.domain.port.UserSessionRoutingPort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class ResolveSessionTargetsUseCase {

    private final UserSessionRoutingPort userSessionRoutingPort;

    public ResolveSessionTargetsUseCase(UserSessionRoutingPort userSessionRoutingPort) {
        this.userSessionRoutingPort = userSessionRoutingPort;
    }

    public Set<String> execute(ResolveSessionTargetsRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Resolve session targets request is required");
        }

        if (request.userIds() == null || request.userIds().isEmpty()) {
            return Set.of();
        }

        Set<String> sanitizedUserIds = request.userIds().stream()
                .filter(userId -> userId != null && !userId.isBlank())
                .map(String::trim)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        if (sanitizedUserIds.isEmpty()) {
            return Set.of();
        }

        return userSessionRoutingPort.findServerIdsByUserIds(sanitizedUserIds);
    }
}
