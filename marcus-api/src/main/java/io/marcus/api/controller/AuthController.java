package io.marcus.api.controller;

import io.marcus.application.dto.LoginRequest;
import io.marcus.application.dto.LoginResponse;
import io.marcus.application.dto.RefreshTokenRequest;
import io.marcus.application.usecase.AuthenticateUserUseCase;
import io.marcus.application.usecase.RefreshAccessTokenUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RefreshAccessTokenUseCase refreshAccessTokenUseCase;

    public AuthController(AuthenticateUserUseCase authenticateUserUseCase,
            RefreshAccessTokenUseCase refreshAccessTokenUseCase) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.refreshAccessTokenUseCase = refreshAccessTokenUseCase;
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
