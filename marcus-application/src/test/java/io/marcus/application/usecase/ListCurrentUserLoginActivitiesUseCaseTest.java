package io.marcus.application.usecase;

import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.port.UserProfileReadPort;
import io.marcus.domain.service.IdentityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCurrentUserLoginActivitiesUseCaseTest {

    @Mock
    private IdentityService identityService;

    @Mock
    private UserProfileReadPort userProfileReadPort;

    private ListCurrentUserLoginActivitiesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListCurrentUserLoginActivitiesUseCase(identityService, userProfileReadPort);
    }

    @Test
    void shouldListCurrentUserLoginActivitiesWithNormalizedPagination() {
        UserProfileReadPort.LoginActivityPageSnapshot page = new UserProfileReadPort.LoginActivityPageSnapshot(
                List.of(),
                new UserProfileReadPort.OffsetPaginationMetaSnapshot(0, 100, 0, 0, false)
        );

        when(identityService.getCurrentUserId()).thenReturn(Optional.of("usr_1"));
        when(userProfileReadPort.listCurrentUserLoginActivities("usr_1", 0, 100)).thenReturn(page);

        UserProfileReadPort.LoginActivityPageSnapshot result = useCase.execute(-1, 300);

        assertThat(result).isEqualTo(page);
    }

    @Test
    void shouldThrowWhenNoAuthenticatedUser() {
        when(identityService.getCurrentUserId()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(0, 20))
                .isInstanceOf(UnauthenticatedException.class)
                .hasMessage("No authenticated user found");
    }
}
