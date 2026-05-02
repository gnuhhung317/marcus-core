package io.marcus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.dto.UpsertBotSubscriberRequest;
import io.marcus.application.dto.UpsertUserSessionRequest;
import io.marcus.application.usecase.RemoveBotSubscriberUseCase;
import io.marcus.application.usecase.RemoveUserSessionUseCase;
import io.marcus.application.usecase.UpsertBotSubscriberUseCase;
import io.marcus.application.usecase.UpsertUserSessionUseCase;
import io.marcus.infrastructure.security.BotSignatureInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RoutingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionsHandler.class)
class RoutingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UpsertUserSessionUseCase upsertUserSessionUseCase;

    @MockBean
    private UpsertBotSubscriberUseCase upsertBotSubscriberUseCase;

    @MockBean
    private RemoveUserSessionUseCase removeUserSessionUseCase;

    @MockBean
    private RemoveBotSubscriberUseCase removeBotSubscriberUseCase;

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
    void shouldReturnNoContentWhenUpsertUserSessionSuccessfully() throws Exception {
        UpsertUserSessionRequest request = new UpsertUserSessionRequest("user-1", "session-1", "server-a");

        mockMvc.perform(post("/routing/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(upsertUserSessionUseCase).execute(request);
    }

    @Test
    void shouldReturnBadRequestWhenUpsertUserSessionUseCaseThrowsIllegalArgument() throws Exception {
        UpsertUserSessionRequest request = new UpsertUserSessionRequest("", "session-1", "server-a");
        doThrow(new IllegalArgumentException("User id is required"))
                .when(upsertUserSessionUseCase)
                                .execute(any(UpsertUserSessionRequest.class));

        mockMvc.perform(post("/api/v1/routing/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("User id is required"))
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldReturnNoContentWhenUpsertBotSubscriberSuccessfully() throws Exception {
        UpsertBotSubscriberRequest request = new UpsertBotSubscriberRequest("bot-1", "user-1");

        mockMvc.perform(post("/routing/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(upsertBotSubscriberUseCase).execute(request);
    }

    @Test
    void shouldReturnBadRequestWhenUpsertBotSubscriberUseCaseThrowsIllegalArgument() throws Exception {
        UpsertBotSubscriberRequest request = new UpsertBotSubscriberRequest("", "user-1");
        doThrow(new IllegalArgumentException("Bot id is required"))
                .when(upsertBotSubscriberUseCase)
                                .execute(any(UpsertBotSubscriberRequest.class));

        mockMvc.perform(post("/api/v1/routing/subscribers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Bot id is required"))
            .andExpect(jsonPath("$.status").value(400));
    }

            @Test
            void shouldReturnNoContentWhenRemoveUserSessionSuccessfully() throws Exception {
            mockMvc.perform(delete("/routing/sessions")
                    .param("userId", "user-1")
                    .param("sessionId", "session-1"))
                .andExpect(status().isNoContent());
            }

            @Test
            void shouldReturnBadRequestWhenRemoveUserSessionUseCaseThrowsIllegalArgument() throws Exception {
            doThrow(new IllegalArgumentException("User id is required"))
                .when(removeUserSessionUseCase)
                .execute(any());

            mockMvc.perform(delete("/api/v1/routing/sessions")
                    .param("userId", "")
                    .param("sessionId", "session-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("User id is required"))
                .andExpect(jsonPath("$.status").value(400));
            }

            @Test
            void shouldReturnNoContentWhenRemoveBotSubscriberSuccessfully() throws Exception {
            mockMvc.perform(delete("/routing/subscribers")
                    .param("botId", "bot-1")
                    .param("userId", "user-1"))
                .andExpect(status().isNoContent());
            }

            @Test
            void shouldReturnBadRequestWhenRemoveBotSubscriberUseCaseThrowsIllegalArgument() throws Exception {
            doThrow(new IllegalArgumentException("Bot id is required"))
                .when(removeBotSubscriberUseCase)
                .execute(any());

            mockMvc.perform(delete("/api/v1/routing/subscribers")
                    .param("botId", "")
                    .param("userId", "user-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Bot id is required"))
                .andExpect(jsonPath("$.status").value(400));
            }
}