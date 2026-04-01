package io.marcus.application.dto;

import java.util.Set;

public record ResolveSessionTargetsRequest(
        Set<String> userIds
) {
}
