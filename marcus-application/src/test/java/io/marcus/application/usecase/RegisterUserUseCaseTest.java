package io.marcus.application.usecase;

import io.marcus.application.dto.RegisterUserRequest;
import io.marcus.application.dto.RegisterUserResponse;
import io.marcus.domain.model.User;
import io.marcus.domain.port.PasswordHashPort;
import io.marcus.domain.port.UserRegistrationPort;
import io.marcus.domain.port.UserUniquenessPort;
import io.marcus.domain.vo.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock
    private UserUniquenessPort userUniquenessPort;

    @Mock
    private PasswordHashPort passwordHashPort;

    @Mock
    private UserRegistrationPort userRegistrationPort;

    @InjectMocks
    private RegisterUserUseCase registerUserUseCase;

    @Test
    void shouldRegisterUserWithRequestedRole() {
        RegisterUserRequest request = new RegisterUserRequest(
                " trader01 ",
                null,
                "secret-password",
                "trader01@example.com",
                Role.DEVELOPER
        );
        when(userUniquenessPort.existsByUsername("trader01")).thenReturn(false);
        when(userUniquenessPort.existsByEmail("trader01@example.com")).thenReturn(false);
        when(passwordHashPort.encode("secret-password")).thenReturn("hashed-password");
        when(userRegistrationPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterUserResponse response = registerUserUseCase.execute(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRegistrationPort).save(userCaptor.capture());
        assertEquals("trader01", userCaptor.getValue().getUsername());
        assertEquals(Role.DEVELOPER, userCaptor.getValue().getRole());
        assertEquals("trader01", response.username());
        assertEquals("trader01@example.com", response.email());
        assertEquals("DEVELOPER", response.role());
    }

    @Test
    void shouldDefaultToUserRoleWhenRoleIsMissing() {
        RegisterUserRequest request = new RegisterUserRequest(
                "user01",
                null,
                "secret-password",
                "user01@example.com",
                null
        );
        when(userUniquenessPort.existsByUsername("user01")).thenReturn(false);
        when(userUniquenessPort.existsByEmail("user01@example.com")).thenReturn(false);
        when(passwordHashPort.encode("secret-password")).thenReturn("hashed-password");
        when(userRegistrationPort.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterUserResponse response = registerUserUseCase.execute(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRegistrationPort).save(userCaptor.capture());
        assertEquals(Role.USER, userCaptor.getValue().getRole());
        assertEquals("USER", response.role());
    }

    @Test
    void shouldRejectDuplicateUsername() {
        RegisterUserRequest request = new RegisterUserRequest(
                "user01",
                null,
                "secret-password",
                "user01@example.com",
                Role.USER
        );
        when(userUniquenessPort.existsByUsername("user01")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> registerUserUseCase.execute(request));
    }
}