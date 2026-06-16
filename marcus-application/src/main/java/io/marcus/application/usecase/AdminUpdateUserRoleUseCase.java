package io.marcus.application.usecase;

import io.marcus.application.dto.AdminDtos;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.model.User;
import io.marcus.domain.port.AdminUserPort;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUpdateUserRoleUseCase {

    private final AdminUserPort adminUserPort;
    private final IdentityService identityService;
    private final AdminRecordAuditEventUseCase adminRecordAuditEventUseCase;

    @Transactional
    public AdminDtos.UserRow execute(String userId, AdminDtos.UpdateUserRoleRequest request) {
        String currentAdminId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User id is required");
        }
        if (request == null || request.role() == null) {
            throw new IllegalArgumentException("Role is required");
        }
        if (request.role() == Role.ADMIN) {
            throw new IllegalArgumentException("ADMIN role cannot be assigned via this endpoint");
        }

        User user = adminUserPort.findByUserId(userId.trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (currentAdminId.equals(user.getUserId())) {
            throw new ForbiddenOperationException("Admins cannot change their own role");
        }

        if (user.getRole() == Role.ADMIN && adminUserPort.countByRoleAndBannedFalse(Role.ADMIN) <= 1) {
            throw new ForbiddenOperationException("Cannot remove the last active admin");
        }

        Role previousRole = user.getRole();
        user.setRole(request.role());

        User saved = adminUserPort.save(user);
        adminRecordAuditEventUseCase.execute(
                currentAdminId,
                "USER_ROLE_UPDATED",
                "USER",
                saved.getUserId(),
                normalizeReason(request.reason()),
                snapshotUser(user, previousRole),
                snapshotUser(saved, saved.getRole())
        );

        return toRow(saved);
    }

    private AdminDtos.UserRow toRow(User user) {
        return new AdminDtos.UserRow(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.isBanned(),
                user.getBannedAt(),
                user.getBannedByUserId(),
                user.getBanReason(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private java.util.Map<String, Object> snapshotUser(User user, Role role) {
        return java.util.Map.of(
                "userId", user.getUserId(),
                "username", user.getUsername(),
                "email", user.getEmail(),
                "role", role.name(),
                "banned", user.isBanned()
        );
    }

    private String normalizeReason(String reason) {
        String normalized = reason == null ? "" : reason.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Reason is required");
        }
        return normalized;
    }
}
