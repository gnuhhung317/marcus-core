package io.marcus.api.controller;

import io.marcus.application.dto.LoginRequest;
import io.marcus.application.dto.LoginResponse;
import io.marcus.application.dto.RefreshTokenRequest;
import io.marcus.application.dto.RegisterUserRequest;
import io.marcus.application.dto.RegisterUserResponse;
import io.marcus.application.usecase.AuthenticateUserUseCase;
import io.marcus.application.usecase.RefreshAccessTokenUseCase;
import io.marcus.application.usecase.RegisterUserUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;
    private final RegisterUserUseCase registerUserUseCase;

    public AuthController(AuthenticateUserUseCase authenticateUserUseCase,
            RefreshAccessTokenUseCase refreshAccessTokenUseCase,
            RegisterUserUseCase registerUserUseCase) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
        this.registerUserUseCase = registerUserUseCase;
    }

    @PostMapping("/register")
    public RegisterUserResponse register(@RequestBody RegisterUserRequest registerUserRequest) {
        return registerUserUseCase.execute(registerUserRequest);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest loginRequest) {
        return authenticateUserUseCase.execute(loginRequest);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        return refreshAccessTokenUseCase.execute(refreshTokenRequest);
    }
}
