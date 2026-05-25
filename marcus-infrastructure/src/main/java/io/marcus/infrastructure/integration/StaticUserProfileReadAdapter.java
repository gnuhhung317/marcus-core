package io.marcus.infrastructure.integration;

import io.marcus.domain.exception.ResourceConflictException;
import io.marcus.domain.port.UserProfileReadPort;
import io.marcus.infrastructure.persistence.SpringDataUserRepository;
import io.marcus.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Component
@RequiredArgsConstructor
public class StaticUserProfileReadAdapter implements UserProfileReadPort {

    private final SpringDataUserRepository springDataUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileSnapshot getCurrentUserProfile(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }

        UserEntity user = springDataUserRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("User profile not found for ID: " + userId));

        return new UserProfileSnapshot(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : "TRADER"
        );
    }

    @Override
    @Transactional
    public UserProfileSnapshot updateCurrentUserProfile(String userId, UserProfileUpdateSnapshot request) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (request == null) {
            throw new IllegalArgumentException("Request body must not be null");
        }

        UserEntity user = springDataUserRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("User profile not found for ID: " + userId));

        String newUsername = request.username();
        if (newUsername != null && !newUsername.isBlank() && !newUsername.equals(user.getUsername())) {
            if (springDataUserRepository.existsByUsername(newUsername)) {
                throw new ResourceConflictException("Username is already taken");
            }
            user.setUsername(newUsername);
        }

        String newEmail = request.email();
        if (newEmail != null && !newEmail.isBlank() && !newEmail.equalsIgnoreCase(user.getEmail())) {
            if (springDataUserRepository.existsByEmail(newEmail)) {
                throw new ResourceConflictException("Email is already taken");
            }
            user.setEmail(newEmail);
        }

        UserEntity savedUser = springDataUserRepository.save(user);

        return new UserProfileSnapshot(
                savedUser.getUserId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole() != null ? savedUser.getRole().name() : "TRADER"
        );
    }
}
