package io.marcus.api.controller.admin;

import io.marcus.application.dto.AdminDtos;
import io.marcus.application.usecase.AdminListUsersUseCase;
import io.marcus.application.usecase.AdminSetUserBanUseCase;
import io.marcus.application.usecase.AdminUpdateUserRoleUseCase;
import io.marcus.domain.vo.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin", "/api/admin", "/api/v1/admin"})
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminListUsersUseCase adminListUsersUseCase;
    private final AdminUpdateUserRoleUseCase adminUpdateUserRoleUseCase;
    private final AdminSetUserBanUseCase adminSetUserBanUseCase;

    @GetMapping("/users")
    public ResponseEntity<AdminDtos.PageResponse<AdminDtos.UserRow>> listUsers(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean banned,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminListUsersUseCase.execute(query, role, banned, page, size));
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<AdminDtos.UserRow> updateUserRole(
            @PathVariable String userId,
            @RequestBody AdminDtos.UpdateUserRoleRequest request
    ) {
        return ResponseEntity.ok(adminUpdateUserRoleUseCase.execute(userId, request));
    }

    @PatchMapping("/users/{userId}/ban")
    public ResponseEntity<AdminDtos.UserRow> updateUserBan(
            @PathVariable String userId,
            @RequestBody AdminDtos.UpdateUserBanRequest request
    ) {
        return ResponseEntity.ok(adminSetUserBanUseCase.execute(userId, request));
    }
}
