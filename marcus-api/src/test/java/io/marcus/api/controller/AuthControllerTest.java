package io.marcus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.application.dto.RegisterUserRequest;
import io.marcus.application.dto.RegisterUserResponse;
import io.marcus.application.usecase.AuthenticateUserUseCase;
import io.marcus.application.usecase.RefreshAccessTokenUseCase;
import io.marcus.application.usecase.RegisterUserUseCase;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.domain.vo.Role;
import io.marcus.infrastructure.security.BotSignatureInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticateUserUseCase authenticateUserUseCase;

    @MockBean
    private RefreshAccessTokenUseCase refreshAccessTokenUseCase;

    @MockBean
    private RegisterUserUseCase registerUserUseCase;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private BotSignatureInterceptor botSignatureInterceptor;

    @Test
    void shouldRegisterUserWithRequestedRole() throws Exception {
        RegisterUserRequest request = new RegisterUserRequest(
                "dev_user",
                null,
                "secret-password",
                "dev_user@example.com",
                Role.DEVELOPER
        );
        RegisterUserResponse response = new RegisterUserResponse(
                "usr_123",
                "trader01",
                "trader01@example.com",
                "DEVELOPER"
        );

        when(registerUserUseCase.execute(any(RegisterUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("usr_123"))
                .andExpect(jsonPath("$.role").value("DEVELOPER"));

        verify(registerUserUseCase).execute(request);
    }
}