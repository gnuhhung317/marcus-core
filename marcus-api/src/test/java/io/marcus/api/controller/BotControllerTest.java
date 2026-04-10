package io.marcus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.dto.BotRegistrationResult;
import io.marcus.application.dto.RegisterBotRequest;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private BotSignatureInterceptor botSignatureInterceptor;

    @Test
    void shouldRegisterBotSuccessfully() throws Exception {
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
}
