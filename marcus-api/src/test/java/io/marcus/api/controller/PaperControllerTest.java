package io.marcus.api.controller;

import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.exception.ResourceConflictException;
import io.marcus.application.usecase.CreatePaperOrderUseCase;
import io.marcus.application.usecase.GetPaperSessionSummaryUseCase;
import io.marcus.application.usecase.ListPaperExecutionLogsUseCase;
import io.marcus.application.usecase.ListPaperSignalsUseCase;
import io.marcus.application.usecase.PausePaperSessionUseCase;
import io.marcus.application.usecase.ResumePaperSessionUseCase;
import io.marcus.domain.port.TerminalReadPort;
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
import org.springframework.http.MediaType;
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

@WebMvcTest(PaperController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionsHandler.class)
class PaperControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GetPaperSessionSummaryUseCase getPaperSessionSummaryUseCase;

    @MockBean
    private ListPaperExecutionLogsUseCase listPaperExecutionLogsUseCase;

    @MockBean
    private ListPaperSignalsUseCase listPaperSignalsUseCase;

    @MockBean
    private CreatePaperOrderUseCase createPaperOrderUseCase;

    @MockBean
    private PausePaperSessionUseCase pausePaperSessionUseCase;

    @MockBean
    private ResumePaperSessionUseCase resumePaperSessionUseCase;

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
    void shouldGetPaperSessionSummary() throws Exception {
        when(getPaperSessionSummaryUseCase.execute())
                .thenReturn(new TerminalReadPort.PaperSessionSummarySnapshot("ps_1", "RUNNING", 10000, 120, 5000));

        mockMvc.perform(get("/api/v1/paper/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void shouldGetPaperLogs() throws Exception {
        TerminalReadPort.PaperExecutionLogPageSnapshot page = new TerminalReadPort.PaperExecutionLogPageSnapshot(
                List.of(new TerminalReadPort.PaperExecutionLogItemSnapshot(LocalDateTime.of(2026, 4, 1, 10, 0), "INFO", "Paper execution event #1")),
                new TerminalReadPort.CursorPaginationMetaSnapshot(null, "paper-cursor-50", 50, true)
        );
        when(listPaperExecutionLogsUseCase.execute(null, 50)).thenReturn(page);

        mockMvc.perform(get("/api/v1/paper/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].level").value("INFO"))
                .andExpect(jsonPath("$.meta.nextCursor").value("paper-cursor-50"));
    }

    @Test
    void shouldGetPaperSignals() throws Exception {
        when(listPaperSignalsUseCase.execute("ALL", 50)).thenReturn(List.of(
                new TerminalReadPort.PaperSignalSnapshot("sig_1", "bot_1", "BTCUSDT", "BUY", 0.7, "ACTIVE", LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/v1/paper/signals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].signalId").value("sig_1"));
    }

    @Test
    void shouldCreatePaperOrder() throws Exception {
        when(createPaperOrderUseCase.execute("BTCUSDT", "LIMIT", "BUY", 0.5, 64500.5))
                .thenReturn(new TerminalReadPort.PaperOrderSnapshot("ord_1", "ACCEPTED", 64500.5));

        mockMvc.perform(post("/api/v1/paper/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetPair\":\"BTCUSDT\",\"orderType\":\"LIMIT\",\"side\":\"BUY\",\"quantity\":0.5,\"limitPrice\":64500.5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value("ord_1"));
    }

    @Test
    void shouldReturnValidationFailedForInvalidOrderType() throws Exception {
        mockMvc.perform(post("/api/v1/paper/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetPair\":\"BTCUSDT\",\"orderType\":\"STOP\",\"side\":\"BUY\",\"quantity\":0.5}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldReturnBadRequestForInvalidMarketPayload() throws Exception {
        when(createPaperOrderUseCase.execute("BTCUSDT", "MARKET", "BUY", 0.5, 64500.5))
                .thenThrow(new IllegalArgumentException("limitPrice must be omitted for MARKET orders"));

        mockMvc.perform(post("/api/v1/paper/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetPair\":\"BTCUSDT\",\"orderType\":\"MARKET\",\"side\":\"BUY\",\"quantity\":0.5,\"limitPrice\":64500.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void shouldPauseAndResumePaperSession() throws Exception {
        when(pausePaperSessionUseCase.execute())
                .thenReturn(new TerminalReadPort.PaperSessionStateSnapshot("ps_1", "PAUSED"));
        when(resumePaperSessionUseCase.execute())
                .thenReturn(new TerminalReadPort.PaperSessionStateSnapshot("ps_1", "RUNNING"));

        mockMvc.perform(post("/api/v1/paper/session/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));

        mockMvc.perform(post("/api/v1/paper/session/resume"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void shouldReturnConflictWhenPauseTransitionInvalid() throws Exception {
        when(pausePaperSessionUseCase.execute())
                .thenThrow(new ResourceConflictException("Paper session is already paused"));

        mockMvc.perform(post("/api/v1/paper/session/pause"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }
}
