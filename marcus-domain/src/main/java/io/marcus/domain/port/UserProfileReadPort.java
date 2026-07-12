package io.marcus.domain.port;

import java.util.List;

public interface UserProfileReadPort {

    record UserProfileSnapshot(String userId, String username, String email, String role) {}

    record UserProfileUpdateSnapshot(String username, String email) {}

    record UserPasswordUpdateSnapshot(String currentPassword, String newPassword) {}

    record OffsetPaginationMetaSnapshot(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {}

    UserProfileSnapshot getCurrentUserProfile(String userId);

    UserProfileSnapshot updateCurrentUserProfile(String userId, UserProfileUpdateSnapshot request);

    UserProfileSnapshot changeCurrentUserPassword(String userId, UserPasswordUpdateSnapshot request);
}
