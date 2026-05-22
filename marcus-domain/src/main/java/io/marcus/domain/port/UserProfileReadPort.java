package io.marcus.domain.port;

import java.time.LocalDateTime;
import java.util.List;

public interface UserProfileReadPort {

    record UserProfileSnapshot(String userId, String username, String email, String role) {}

    record UserProfileUpdateSnapshot(String username, String email) {}

    record LoginActivitySnapshot(
            LocalDateTime occurredAt,
            String ipAddress,
            String userAgent,
            boolean success
    ) {}

    record LoginActivityPageSnapshot(
            List<LoginActivitySnapshot> items,
            OffsetPaginationMetaSnapshot meta
    ) {}

    record OffsetPaginationMetaSnapshot(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {}

    UserProfileSnapshot getCurrentUserProfile(String userId);

    UserProfileSnapshot updateCurrentUserProfile(String userId, UserProfileUpdateSnapshot request);

    LoginActivityPageSnapshot listCurrentUserLoginActivities(String userId, int page, int size);
}
