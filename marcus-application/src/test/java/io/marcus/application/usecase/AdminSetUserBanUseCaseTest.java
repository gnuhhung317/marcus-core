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
class AdminSetUserBanUseCaseTest {

    @Mock
    private AdminUserPort adminUserPort;

    @Mock
    private IdentityService identityService;

    @Mock
    private AdminRecordAuditEventUseCase adminRecordAuditEventUseCase;

    private AdminSetUserBanUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AdminSetUserBanUseCase(adminUserPort, identityService, adminRecordAuditEventUseCase);
    }

    @Test
    void shouldRejectSelfBan() {
        User user = User.builder().userId("admin-1").role(Role.ADMIN).banned(false).build();
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("admin-1"));
        when(adminUserPort.findByUserId("admin-1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> useCase.execute("admin-1", AdminDtos.UpdateUserBanRequest.builder().banned(true).reason("test").build()))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Admins cannot ban themselves");

        verify(adminUserPort, never()).save(any());
        verifyNoInteractions(adminRecordAuditEventUseCase);
    }

    @Test
    void shouldBanUserAndAuditChange() {
        User user = User.builder().userId("user-1").username("alice").email("alice@example.com").role(Role.TRADER).banned(false).build();
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("admin-1"));
        when(adminUserPort.findByUserId("user-1")).thenReturn(Optional.of(user));
        when(adminUserPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminDtos.UserRow result = useCase.execute("user-1", AdminDtos.UpdateUserBanRequest.builder().banned(true).reason("abuse").build());

        assertThat(result.banned()).isTrue();
        verify(adminUserPort).save(user);
        verify(adminRecordAuditEventUseCase).execute(
                eq("admin-1"),
                eq("USER_BANNED"),
                eq("USER"),
                eq("user-1"),
                eq("abuse"),
                any(),
                any()
        );
    }
}
