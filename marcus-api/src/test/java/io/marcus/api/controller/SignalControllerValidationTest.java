package io.marcus.api.controller;

import io.marcus.application.dto.CaptureSignalRequest;
import io.marcus.application.usecase.CaptureSignalUseCase;
import io.marcus.application.usecase.ListSignalsUseCase;
import io.marcus.api.exception.GlobalExceptionsHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SignalControllerValidationTest {

    @Mock
    private CaptureSignalUseCase captureSignalUseCase;

    @Mock
    private ListSignalsUseCase listSignalsUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new SignalController(captureSignalUseCase, listSignalsUseCase))
                .setControllerAdvice(new GlobalExceptionsHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturn422ForInvalidCaptureSignalPayload() throws Exception {
        mockMvc.perform(post("/api/v1/signals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"signalId\":\"\"," +
                                "\"botId\":\"bot-1\"," +
                                "\"symbol\":\"BTCUSDT\"," +
                                "\"action\":\"OPEN_LONG\"," +
                                "\"orderType\":\"LIMIT\"," +
                                "\"generatedTimestamp\":\"2026-05-24T10:15:30\"" +
                                "}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors").isArray());

        verify(captureSignalUseCase, never()).execute(org.mockito.ArgumentMatchers.any(CaptureSignalRequest.class));
    }
}