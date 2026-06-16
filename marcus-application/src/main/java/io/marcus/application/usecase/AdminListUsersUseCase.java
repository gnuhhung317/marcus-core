package io.marcus.application.usecase;

import io.marcus.application.dto.AdminDtos;
import io.marcus.domain.port.AdminUserPort;
import io.marcus.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminListUsersUseCase {

    private final AdminUserPort adminUserPort;

    public AdminDtos.PageResponse<AdminDtos.UserRow> execute(String query, Role role, Boolean banned, int page, int size) {
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, 100));

        var result = adminUserPort.search(query, role, banned, normalizedPage, normalizedSize);
        return new AdminDtos.PageResponse<>(
                result.getContent().stream().map(user -> new AdminDtos.UserRow(
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
                )).toList(),
                result.getTotalElements(),
                result.getNumber(),
                result.getSize(),
                result.hasNext()
        );
    }
}
