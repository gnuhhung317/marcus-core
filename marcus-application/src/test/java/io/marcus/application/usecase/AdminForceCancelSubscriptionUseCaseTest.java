package io.marcus.application.usecase;

import io.marcus.application.dto.AdminDtos;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.domain.model.UserSubscription;
import io.marcus.domain.port.AdminSubscriptionPort;
import io.marcus.domain.service.IdentityService;
import io.marcus.domain.vo.SubscriptionStatus;
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
class AdminForceCancelSubscriptionUseCaseTest {

    @Mock
    private AdminSubscriptionPort adminSubscriptionPort;

    @Mock
    private IdentityService identityService;

    @Mock
    private AdminRecordAuditEventUseCase adminRecordAuditEventUseCase;

    private AdminForceCancelSubscriptionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AdminForceCancelSubscriptionUseCase(adminSubscriptionPort, identityService, adminRecordAuditEventUseCase);
    }

    @Test
    void shouldRejectNonActiveSubscription() {
        UserSubscription subscription = UserSubscription.builder().userSubscriptionId("sub-1").status(SubscriptionStatus.CANCELED).build();
        when(identityService.getCurrentUserId()).thenReturn(Optional.of("admin-1"));
        when(adminSubscriptionPort.findByUserSubscriptionId("sub-1")).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> useCase.execute("sub-1", AdminDtos.ForceCancelSubscriptionRequest.builder().reason("test").build()))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("Only active subscriptions can be force canceled");

        verify(adminSubscriptionPort, never()).forceCancel(any(), any(), any());
        verifyNoInteractions(adminRecordAuditEventUseCase);
    }

    @Test
    void shouldForceCancelAndAudit() {
        UserSubscription subscription = UserSubscription.builder()
                .userSubscriptionId("sub-1")
                .userId("user-1")
                .botId("bot-1")
                .status(SubscriptionStatus.ACTIVE)
                .executorConnected(true)
                .build();
        UserSubscription canceled = UserSubscription.builder()
                .userSubscriptionId("sub-1")
                .userId("user-1")
                .botId("bot-1")
                .status(SubscriptionStatus.CANCELED)
                .executorConnected(false)
                .canceledByAdminId("admin-1")
                .cancellationReason("abuse")
                .build();

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("admin-1"));
        when(adminSubscriptionPort.findByUserSubscriptionId("sub-1")).thenReturn(Optional.of(subscription), Optional.of(canceled));

        AdminDtos.BotSubscriberRow result = useCase.execute("sub-1", AdminDtos.ForceCancelSubscriptionRequest.builder().reason("abuse").build());

        assertThat(result.status()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(result.canceledByAdminId()).isEqualTo("admin-1");
        verify(adminSubscriptionPort).forceCancel("sub-1", "admin-1", "abuse");
        verify(adminRecordAuditEventUseCase).execute(
                eq("admin-1"),
                eq("SUBSCRIPTION_FORCE_CANCELED"),
                eq("SUBSCRIPTION"),
                eq("sub-1"),
                eq("abuse"),
                any(),
                any()
        );
    }
}
