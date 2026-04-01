package io.marcus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.dto.BotSummaryResult;
import io.marcus.application.dto.BotRegistrationResult;
import io.marcus.application.dto.RegisterBotRequest;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.usecase.ListDeveloperBotsUseCase;
import io.marcus.application.usecase.ListPublicBotsUseCase;
import io.marcus.application.usecase.RegisterBotUseCase;
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

        when(listPublicBotsUseCase.execute()).thenReturn(response);

        mockMvc.perform(get("/api/v1/bots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].botId").value("bot_001"))
                .andExpect(jsonPath("$[0].apiKey").doesNotExist());
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
        BotRegistrationResult response = BotRegistrationResult.builder()
                .botId("bot_123")
                .botName("Scalp Bot")
                .description("Scalp strategy")
                .apiKey("ak_123")
                .rawSecret("sk_raw")
                .status("ACTIVE")
                .tradingPair("BTCUSDT")
                .exchange("binance")
                .build();

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
}
