package io.marcus.application.usecase;

import io.marcus.application.dto.AdminDtos;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.domain.model.User;
import io.marcus.domain.port.AdminUserPort;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUpdateUserRoleUseCaseTest {

    @Mock
    private AdminUserPort adminUserPort;

    @Mock
    private IdentityService identityService;

    @Mock
    private AdminRecordAuditEventUseCase adminRecordAuditEventUseCase;

    private AdminUpdateUserRoleUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AdminUpdateUserRoleUseCase(adminUserPort, identityService, adminRecordAuditEventUseCase);
    }

    @Test
    void shouldRejectAdminRoleAssignment() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("admin-1"));

        assertThatThrownBy(() -> useCase.execute("user-1", AdminDtos.UpdateUserRoleRequest.builder().role(Role.ADMIN).reason("test").build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ADMIN role cannot be assigned via this endpoint");

        verifyNoInteractions(adminUserPort, adminRecordAuditEventUseCase);
    }

    @Test
    void shouldRejectSelfRoleChange() {
        User user = User.builder().userId("admin-1").role(Role.ADMIN).banned(false).build();
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("admin-1"));
        when(adminUserPort.findByUserId("admin-1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.execute("admin-1", AdminDtos.UpdateUserRoleRequest.builder().role(Role.DEVELOPER).reason("test").build()))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Admins cannot change their own role");

        verify(adminUserPort, never()).save(any());
        verifyNoInteractions(adminRecordAuditEventUseCase);
    }

    @Test
    void shouldRejectRemovingLastActiveAdmin() {
        User user = User.builder().userId("admin-2").role(Role.ADMIN).banned(false).build();
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("admin-1"));
        when(adminUserPort.findByUserId("admin-2")).thenReturn(Optional.of(user));
        when(adminUserPort.countByRoleAndBannedFalse(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> useCase.execute("admin-2", AdminDtos.UpdateUserRoleRequest.builder().role(Role.TRADER).reason("test").build()))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Cannot remove the last active admin");

        verify(adminUserPort, never()).save(any());
        verifyNoInteractions(adminRecordAuditEventUseCase);
    }

    @Test
    void shouldUpdateRoleAndAuditChange() {
        User user = User.builder().userId("user-1").username("alice").email("alice@example.com").role(Role.TRADER).banned(false).build();
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("admin-1"));
        when(adminUserPort.findByUserId("user-1")).thenReturn(Optional.of(user));
        when(adminUserPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminDtos.UserRow result = useCase.execute("user-1", AdminDtos.UpdateUserRoleRequest.builder().role(Role.DEVELOPER).reason("promotion").build());

        assertThat(result.role()).isEqualTo(Role.DEVELOPER);
        verify(adminUserPort).save(user);
        verify(adminRecordAuditEventUseCase).execute(
                eq("admin-1"),
                eq("USER_ROLE_UPDATED"),
                eq("USER"),
                eq("user-1"),
                eq("promotion"),
                any(),
                any()
        );
    }
}
