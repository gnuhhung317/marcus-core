package io.marcus.api.controller;

import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.dto.MySubscriptionsResult;
import io.marcus.application.dto.SubscribeBotResult;
import io.marcus.application.dto.SubscriptionSummaryResult;
import io.marcus.application.usecase.ListMySubscriptionsUseCase;
import io.marcus.application.usecase.SubscribeBotUseCase;
import io.marcus.domain.exception.BotNotFoundException;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionsHandler.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscribeBotUseCase subscribeBotUseCase;

    @MockBean
    private ListMySubscriptionsUseCase listMySubscriptionsUseCase;

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
    void shouldSubscribeBotSuccessfully() throws Exception {
        SubscribeBotResult result = SubscribeBotResult.builder()
                .botId("bot_1")
                .wsToken("ws_abc")
                .status("ACTIVE")
                .build();

        when(subscribeBotUseCase.execute("bot_1")).thenReturn(result);

        mockMvc.perform(post("/api/v1/subscriptions/bot_1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.botId").value("bot_1"))
                .andExpect(jsonPath("$.wsToken").value("ws_abc"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldListMySubscriptionsSuccessfully() throws Exception {
        MySubscriptionsResult result = MySubscriptionsResult.builder()
                .wsToken("ws_abc")
                .localExecutorInstruction("Copy token")
                .subscriptions(List.of(
                        SubscriptionSummaryResult.builder()
                                .botId("bot_1")
                                .botName("Momentum Bot")
                                .tradingPair("BTCUSDT")
                                .status("ACTIVE")
                                .subscribedAt(LocalDateTime.of(2026, 4, 1, 2, 0))
                                .build()
                ))
                .build();

        when(listMySubscriptionsUseCase.execute()).thenReturn(result);

        mockMvc.perform(get("/api/v1/subscriptions/my-subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wsToken").value("ws_abc"))
                .andExpect(jsonPath("$.subscriptions[0].botId").value("bot_1"));
    }

    @Test
    void shouldReturnNotFoundWhenBotMissing() throws Exception {
        when(subscribeBotUseCase.execute("bot_missing"))
                .thenThrow(new BotNotFoundException("Bot not found: bot_missing"));

        mockMvc.perform(post("/subscriptions/bot_missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Bot not found: bot_missing"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
