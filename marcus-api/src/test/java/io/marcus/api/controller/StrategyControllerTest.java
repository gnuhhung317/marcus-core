package io.marcus.api.controller;

import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.usecase.FavoriteStrategyUseCase;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.infrastructure.security.BotSignatureInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StrategyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionsHandler.class)
class StrategyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FavoriteStrategyUseCase favoriteStrategyUseCase;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private BotSignatureInterceptor botSignatureInterceptor;

    @Test
    void shouldFavoriteStrategySuccessfully() throws Exception {
        TerminalReadPort.FavoriteStrategySnapshot response = new TerminalReadPort.FavoriteStrategySnapshot("strat-123", true);
        when(favoriteStrategyUseCase.execute("strat-123")).thenReturn(response);

        mockMvc.perform(post("/strategies/strat-123/favorite"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategyId").value("strat-123"))
                .andExpect(jsonPath("$.favorited").value(true));

        verify(favoriteStrategyUseCase).execute("strat-123");
    }
}