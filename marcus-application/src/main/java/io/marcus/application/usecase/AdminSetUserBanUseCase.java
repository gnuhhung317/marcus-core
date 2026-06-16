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

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminSetUserBanUseCase {

    private final AdminUserPort adminUserPort;
    private final IdentityService identityService;
    private final AdminRecordAuditEventUseCase adminRecordAuditEventUseCase;

    @Transactional
    public AdminDtos.UserRow execute(String userId, AdminDtos.UpdateUserBanRequest request) {
        String currentAdminId = identityService.getCurrentUserId()
                .orElseThrow(() -> new UnauthenticatedException("No authenticated user found"));

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User id is required");
        }
        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new IllegalArgumentException("Reason is required");
        }

        User user = adminUserPort.findByUserId(userId.trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        if (currentAdminId.equals(user.getUserId())) {
            throw new ForbiddenOperationException("Admins cannot ban themselves");
        }

        if (request.banned() && user.getRole() == Role.ADMIN && adminUserPort.countByRoleAndBannedFalse(Role.ADMIN) <= 1) {
            throw new ForbiddenOperationException("Cannot ban the last active admin");
        }

        java.util.Map<String, Object> before = snapshot(user);
        user.setBanned(request.banned());
        user.setBannedAt(request.banned() ? LocalDateTime.now() : null);
        user.setBannedByUserId(request.banned() ? currentAdminId : null);
        user.setBanReason(request.banned() ? request.reason().trim() : null);

        User saved = adminUserPort.save(user);
        adminRecordAuditEventUseCase.execute(
                currentAdminId,
                request.banned() ? "USER_BANNED" : "USER_UNBANNED",
                "USER",
                saved.getUserId(),
                request.reason().trim(),
                before,
                snapshot(saved)
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

    private java.util.Map<String, Object> snapshot(User user) {
        java.util.Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("userId", user.getUserId());
        snapshot.put("username", user.getUsername());
        snapshot.put("email", user.getEmail());
        snapshot.put("role", user.getRole() != null ? user.getRole().name() : null);
        snapshot.put("banned", user.isBanned());
        snapshot.put("bannedAt", user.getBannedAt());
        snapshot.put("bannedByUserId", user.getBannedByUserId());
        snapshot.put("banReason", user.getBanReason());
        return snapshot;
    }
}
