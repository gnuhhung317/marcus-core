package io.marcus.domain.port;

import java.util.List;

public interface UserProfileReadPort {

    record UserProfileSnapshot(String userId, String username, String email, String role) {}

    record UserProfileUpdateSnapshot(String username, String email) {}

    record OffsetPaginationMetaSnapshot(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {}

    UserProfileSnapshot getCurrentUserProfile(String userId);

    UserProfileSnapshot updateCurrentUserProfile(String userId, UserProfileUpdateSnapshot request);
}
