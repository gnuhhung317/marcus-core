package io.marcus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.dto.BotRegistrationResult;
import io.marcus.application.dto.RegisterBotRequest;
import io.marcus.application.usecase.ListPublicBotsUseCase;
import io.marcus.application.usecase.RegisterBotUseCase;
import io.marcus.domain.port.AccessTokenPort;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.infrastructure.security.BotSignatureInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BotController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionsHandler.class)
class BotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RegisterBotUseCase registerBotUseCase;

    @MockBean
    private ListPublicBotsUseCase listPublicBotsUseCase;

    @MockBean
    private AccessTokenPort accessTokenPort;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private BotSignatureInterceptor botSignatureInterceptor;

    @Test
    void shouldRegisterBotSuccessfully() throws Exception {
        RegisterBotRequest request = new RegisterBotRequest("Scalp strategy", "BTCUSDT", "Scalp Bot", "binance");
        BotRegistrationResult response = new BotRegistrationResult(
                "bot_123",
                "Scalp Bot",
                "Scalp strategy",
                "Scalp Bot",
                "ak_123",
                "sk_raw",
                "ACTIVE",
                "BTCUSDT",
                "binance"
        );

        when(registerBotUseCase.execute(any(RegisterBotRequest.class))).thenReturn(response);

        mockMvc.perform(post("/bots/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.botId").value("bot_123"))
                .andExpect(jsonPath("$.apiKey").value("ak_123"));

        verify(registerBotUseCase).execute(request);
    }

    @Test
    void shouldReturnBadRequestWhenUseCaseThrowsIllegalArgument() throws Exception {
        RegisterBotRequest request = new RegisterBotRequest(null, "BTCUSDT", "", "binance");
        when(registerBotUseCase.execute(any(RegisterBotRequest.class)))
                .thenThrow(new IllegalArgumentException("Bot name is required"));

        mockMvc.perform(post("/bots/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Bot name is required"));
    }

    @Test
    void shouldListPublicBotsWithCommonFilterCombination() throws Exception {
        TerminalReadPort.BotDiscoveryPageSnapshot response = new TerminalReadPort.BotDiscoveryPageSnapshot(
                java.util.List.of(new TerminalReadPort.BotDiscoverySnapshot(
                        "bot-discovery-003",
                        "Orbit Breakout",
                        "Volatility breakout on SOL markets",
                        "SOLUSDT",
                        "HIGH",
                        0.356,
                        0.218,
                        1525
                )),
                new TerminalReadPort.OffsetPaginationMetaSnapshot(1, 10, 1, 1, false)
        );
        when(listPublicBotsUseCase.execute(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/bots")
                .queryParam("q", "orbit")
                .queryParam("asset", "SOLUSDT")
                .queryParam("risk", "HIGH")
                .queryParam("sort", "-return")
                .queryParam("page", "1")
                .queryParam("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].botId").value("bot-discovery-003"))
                .andExpect(jsonPath("$.items[0].asset").value("SOLUSDT"))
                .andExpect(jsonPath("$.items[0].risk").value("HIGH"))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.size").value(10));

        verify(listPublicBotsUseCase).execute("orbit", "SOLUSDT", "HIGH", "-return", 1, 10);
    }
}
