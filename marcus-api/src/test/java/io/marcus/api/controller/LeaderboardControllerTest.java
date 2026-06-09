package io.marcus.api.controller;

import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.usecase.ListLeaderboardFeaturedUseCase;
import io.marcus.application.usecase.ListLeaderboardSpotlightsUseCase;
import io.marcus.application.usecase.ListLeaderboardBotsUseCase;
import io.marcus.domain.port.BotDiscoveryReadPort;
import io.marcus.domain.port.UserProfileReadPort;
import io.marcus.infrastructure.security.BotSignatureInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaderboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionsHandler.class)
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ListLeaderboardBotsUseCase listLeaderboardBotsUseCase;

    @MockBean
    private ListLeaderboardFeaturedUseCase listLeaderboardFeaturedUseCase;

    @MockBean
    private ListLeaderboardSpotlightsUseCase listLeaderboardSpotlightsUseCase;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private BotSignatureInterceptor botSignatureInterceptor;

    @BeforeEach
    void allowSignatureInterceptor() throws Exception {
        doAnswer(invocation -> true)
                .when(botSignatureInterceptor)
                .preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any());
    }

    @Test
    void shouldGetLeaderboardBots() throws Exception {
        BotDiscoveryReadPort.LeaderboardBotSnapshot item = new BotDiscoveryReadPort.LeaderboardBotSnapshot(
                1, "bot_1", "Momentum 1", "Marcus Desk", 0.32, 2.1, 0.15, "DRY_RUN"
        );
        UserProfileReadPort.OffsetPaginationMetaSnapshot meta = new UserProfileReadPort.OffsetPaginationMetaSnapshot(
                0, 20, 1, 1, false
        );

        when(listLeaderboardBotsUseCase.execute("7D", "CRYPTO", "BTCUSDT", "sharpe", 0, 20))
                .thenReturn(new BotDiscoveryReadPort.LeaderboardBotsPageSnapshot(List.of(item), meta));

        mockMvc.perform(get("/api/v1/leaderboard/bots")
                        .param("timeframe", "7D")
                        .param("market", "CRYPTO")
                        .param("asset", "BTCUSDT")
                        .param("rankMetric", "sharpe")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].botId").value("bot_1"))
                .andExpect(jsonPath("$.meta.page").value(0));
    }

    @Test
    void shouldGetLeaderboardFeatured() throws Exception {
        when(listLeaderboardFeaturedUseCase.execute())
                .thenReturn(new BotDiscoveryReadPort.LeaderboardFeaturedSnapshot(List.of(
                        new BotDiscoveryReadPort.LeaderboardFeaturedItemSnapshot("stg_1", "Apex", "TOP 1", 2.4)
                )));

        mockMvc.perform(get("/api/v1/leaderboard/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].rankLabel").value("TOP 1"));
    }

    @Test
    void shouldGetLeaderboardSpotlights() throws Exception {
        when(listLeaderboardSpotlightsUseCase.execute())
                .thenReturn(List.of(
                        new BotDiscoveryReadPort.BotSpotlightSnapshot("bot_1", "Neutron", "CRYPTO", 0.03)
                ));

        mockMvc.perform(get("/api/v1/leaderboard/spotlights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].botName").value("Neutron"));
    }

    @Test
    void shouldReturnBadRequestWhenStrategiesRequestInvalid() throws Exception {
        when(listLeaderboardBotsUseCase.execute(null, null, null, null, -1, 0))
                .thenThrow(new IllegalArgumentException("Invalid pagination"));

        mockMvc.perform(get("/api/v1/leaderboard/bots")
                        .param("page", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }
}
