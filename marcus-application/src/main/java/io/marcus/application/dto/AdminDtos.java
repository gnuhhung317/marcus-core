package io.marcus.application.dto;

import io.marcus.domain.vo.BotStatus;
import io.marcus.domain.vo.Role;
import io.marcus.domain.vo.SubscriptionStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record PageResponse<T>(
            List<T> items,
            long totalElements,
            int page,
            int size,
            boolean hasNext
    ) {
    }

    public record UserRow(
            String userId,
            String username,
            String email,
            Role role,
            boolean banned,
            LocalDateTime bannedAt,
            String bannedByUserId,
            String banReason,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record BotRow(
            String botId,
            String name,
            String developerId,
            String developerUsername,
            String status,
            String tradingPair,
            String exchangeId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            long activeSubscriberCount
    ) {
    }

    public record BotDetail(
            String botId,
            String name,
            String developerId,
            String developerUsername,
            String description,
            BotStatus status,
            String tradingPair,
            String exchangeId,
            BigDecimal price,
            String riskLevel,
            List<String> assetPairs,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            long activeSubscriberCount,
            long totalSubscriberCount
    ) {
    }

    public record BotSubscriberRow(
            String userSubscriptionId,
            String userId,
            String username,
            String email,
            SubscriptionStatus status,
            boolean executorConnected,
            String packageId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            String canceledByAdminId,
            String cancellationReason,
            LocalDateTime canceledAt
    ) {
    }

    public record AuditEventRow(
            String adminAuditEventId,
            String actorUserId,
            String action,
            String targetType,
            String targetId,
            String reason,
            String beforeStateJson,
            String afterStateJson,
            LocalDateTime createdAt
    ) {
    }

    public record SystemOverview(
            long totalUsers,
            long bannedUsers,
            long totalBots,
            long activeBots,
            long pausedBots,
            long deletedBots,
            long activeSubscriptions,
            long disconnectedExecutors,
            List<AuditEventRow> recentActions,
            String systemHealth,
            LocalDateTime checkedAt
    ) {
    }

    @Builder
    public record UpdateUserRoleRequest(Role role, String reason) {
    }

    @Builder
    public record UpdateUserBanRequest(boolean banned, String reason) {
    }

    @Builder
    public record UpdateBotStatusRequest(BotStatus status, String reason, boolean cancelActiveSubscriptions) {
    }

    @Builder
    public record ForceCancelSubscriptionRequest(String reason) {
    }
}
