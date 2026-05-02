package io.marcus.infrastructure.messaging;

import io.marcus.domain.model.Signal;

public record RoutingDispatchMessage(
        String targetServerId,
        Signal signal
) {
}