package io.marcus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.dto.BotSummaryResult;
import io.marcus.application.dto.BotRegistrationResult;
import io.marcus.application.dto.RegisterBotRequest;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.usecase.GetBotDetailUseCase;
import io.marcus.application.usecase.ListDeveloperBotsUseCase;
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

import java.util.List;

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
    private ListDeveloperBotsUseCase listDeveloperBotsUseCase;

    @MockBean
    private GetBotDetailUseCase getBotDetailUseCase;

    @MockBean
    private AccessTokenPort accessTokenPort;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private BotSignatureInterceptor botSignatureInterceptor;

    @Test
    void shouldReturnPublicBots() throws Exception {
        List<BotSummaryResult> response = List.of(
                BotSummaryResult.builder()
                        .botId("bot_001")
                        .botName("Public Bot")
                        .status("ACTIVE")
                        .tradingPair("BTCUSDT")
                        .exchange("binance")
                        .build()
        );

        when(listPublicBotsUseCase.execute(null, null, null, "-return", 0, 20)).thenReturn(
                new TerminalReadPort.BotDiscoveryPageSnapshot(
                        List.of(),
                        new TerminalReadPort.OffsetPaginationMetaSnapshot(0, 20, 0, 0, false)
                )
        );

        mockMvc.perform(get("/api/v1/bots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page").value(0));

        verify(listPublicBotsUseCase).execute(null, null, null, "-return", 0, 20);
    }

    @Test
    void shouldReturnMyBots() throws Exception {
        List<BotSummaryResult> response = List.of(
                BotSummaryResult.builder()
                        .botId("bot_123")
                        .botName("My Bot")
                        .status("ACTIVE")
                        .tradingPair("BTCUSDT")
                        .exchange("binance")
                        .apiKey("ak_123")
                        .build()
        );

        when(listDeveloperBotsUseCase.execute()).thenReturn(response);

        mockMvc.perform(get("/api/v1/bots/my-bots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].botId").value("bot_123"))
                .andExpect(jsonPath("$[0].apiKey").value("ak_123"));
    }

    @Test
    void shouldReturnCreatedWhenRegisterBotSuccessfully() throws Exception {
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

        mockMvc.perform(post("/api/v1/bots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.botId").value("bot_123"))
                .andExpect(jsonPath("$.apiKey").value("ak_123"));

        verify(registerBotUseCase).execute(request);
    }

    @Test
    void shouldReturnBadRequestWhenUseCaseThrowsIllegalArgument() throws Exception {
        RegisterBotRequest request = new RegisterBotRequest(null, "BTCUSDT", "", "binance");
        when(registerBotUseCase.execute(any(RegisterBotRequest.class)))
                .thenThrow(new IllegalArgumentException("Bot name is required"));

        mockMvc.perform(post("/api/v1/bots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Bot name is required"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturnForbiddenWhenUseCaseThrowsForbiddenOperation() throws Exception {
        RegisterBotRequest request = new RegisterBotRequest("Scalp strategy", "BTCUSDT", "Scalp Bot", "binance");
        when(registerBotUseCase.execute(any(RegisterBotRequest.class)))
                .thenThrow(new ForbiddenOperationException("Only developer can register bot"));

        mockMvc.perform(post("/api/v1/bots")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Only developer can register bot"))
                .andExpect(jsonPath("$.status").value(403));
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
