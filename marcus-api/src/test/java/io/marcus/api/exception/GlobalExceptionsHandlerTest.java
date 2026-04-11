package io.marcus.api.exception;

import io.marcus.application.exception.ResourceConflictException;
import io.marcus.application.exception.ForbiddenOperationException;
import io.marcus.application.exception.UnauthenticatedException;
import io.marcus.domain.exception.BotNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionsHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(new TestErrorController())
                .setControllerAdvice(new GlobalExceptionsHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void shouldReturnCanonicalBadRequestWithProvidedTraceId() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/illegal")
                        .header("X-Trace-Id", "trace-123"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Trace-Id", "trace-123"))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid payload"))
                .andExpect(jsonPath("$.traceId").value("trace-123"));
    }

    @Test
    void shouldReturnCanonicalNotFoundPayload() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Bot not found: bot_missing"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldReturnConflictPayload() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Resource already exists"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldReturnUnauthorizedPayload() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("No authenticated user found"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/test-errors/unauthorized"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldReturnForbiddenPayload() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Only trader can view subscriptions"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value("/api/v1/test-errors/forbidden"))
                .andExpect(jsonPath("$.traceId").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void shouldReturnNotFoundPayload() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/not-found-generic"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Missing resource"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldReturnValidationErrorListForRequestBodyViolations() throws Exception {
        mockMvc.perform(post("/api/v1/test-errors/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].reason").isNotEmpty())
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void shouldReturnInternalServerErrorPayload() throws Exception {
        mockMvc.perform(get("/api/v1/test-errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("Internal server error"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @RestController
    @RequestMapping("/api/v1/test-errors")
    static class TestErrorController {

        @GetMapping("/illegal")
        String illegal() {
            throw new IllegalArgumentException("Invalid payload");
        }

        @GetMapping("/not-found")
        String notFound() {
            throw new BotNotFoundException("Bot not found: bot_missing");
        }

        @GetMapping("/conflict")
        String conflict() {
            throw new ResourceConflictException("Resource already exists");
        }

        @GetMapping("/unauthorized")
        String unauthorized() {
            throw new UnauthenticatedException("No authenticated user found");
        }

        @GetMapping("/forbidden")
        String forbidden() {
            throw new ForbiddenOperationException("Only trader can view subscriptions");
        }

        @GetMapping("/not-found-generic")
        String notFoundGeneric() {
            throw new NoSuchElementException("Missing resource");
        }

        @PostMapping("/validate")
        String validate(@Valid @RequestBody ValidationRequest request) {
            return request.name();
        }

        @GetMapping("/unexpected")
        String unexpected() {
            throw new RuntimeException("boom");
        }
    }

    record ValidationRequest(@NotBlank String name) {
    }
}
