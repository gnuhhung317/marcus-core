package io.marcus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.dto.CreateApiKeyRequest;
import io.marcus.application.dto.UpdateUserPreferencesRequest;
import io.marcus.application.usecase.CreateCurrentUserApiKeyUseCase;
import io.marcus.application.usecase.DeleteCurrentUserApiKeyUseCase;
import io.marcus.application.usecase.GetCurrentUserProfileUseCase;
import io.marcus.application.usecase.ListCurrentUserApiKeysUseCase;
import io.marcus.application.usecase.ListCurrentUserLoginActivitiesUseCase;
import io.marcus.application.usecase.UpdateCurrentUserPreferencesUseCase;
import io.marcus.domain.port.TerminalReadPort;
import io.marcus.infrastructure.security.BotSignatureInterceptor;
import jakarta.servlet.FilterChain;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionsHandler.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GetCurrentUserProfileUseCase getCurrentUserProfileUseCase;

    @MockBean
    private ListCurrentUserApiKeysUseCase listCurrentUserApiKeysUseCase;

    @MockBean
    private UpdateCurrentUserPreferencesUseCase updateCurrentUserPreferencesUseCase;

    @MockBean
    private CreateCurrentUserApiKeyUseCase createCurrentUserApiKeyUseCase;

    @MockBean
    private DeleteCurrentUserApiKeyUseCase deleteCurrentUserApiKeyUseCase;

    @MockBean
    private ListCurrentUserLoginActivitiesUseCase listCurrentUserLoginActivitiesUseCase;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private BotSignatureInterceptor botSignatureInterceptor;

    @BeforeEach
    void setUpFilters() throws Exception {
        doAnswer(invocation -> {
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        doAnswer(invocation -> true)
                .when(botSignatureInterceptor)
                .preHandle(any(), any(), any());
    }

    @Test
    void shouldGetCurrentUserProfile() throws Exception {
        when(getCurrentUserProfileUseCase.execute())
                .thenReturn(new TerminalReadPort.UserProfileSnapshot("usr_1", "trader_1", "trader@marcus.local", "USER"));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("usr_1"));
    }

    @Test
    void shouldUpdateCurrentUserPreferences() throws Exception {
        UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest("UTC", "en-US", true);
        when(updateCurrentUserPreferencesUseCase.execute(any(UpdateUserPreferencesRequest.class)))
                .thenReturn(new TerminalReadPort.UserPreferencesSnapshot("UTC", "en-US", true));

        mockMvc.perform(put("/api/v1/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("UTC"));
    }

    @Test
    void shouldListCurrentUserApiKeys() throws Exception {
        when(listCurrentUserApiKeysUseCase.execute())
                .thenReturn(List.of(
                        new TerminalReadPort.ApiKeySummarySnapshot(
                                "key_1",
                                "Terminal",
                                "mk_live_****ab",
                                LocalDateTime.of(2026, 4, 1, 1, 0),
                                LocalDateTime.of(2026, 4, 2, 1, 0)
                        )
                ));

        mockMvc.perform(get("/api/v1/users/me/api-keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].apiKeyId").value("key_1"));
    }

    @Test
    void shouldCreateCurrentUserApiKey() throws Exception {
        CreateApiKeyRequest request = new CreateApiKeyRequest("Terminal");
        when(createCurrentUserApiKeyUseCase.execute(any(CreateApiKeyRequest.class)))
                .thenReturn(new TerminalReadPort.CreateApiKeySnapshot("key_1", "mk_live_secret", "Terminal"));

        mockMvc.perform(post("/api/v1/users/me/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("mk_live_secret"));
    }

    @Test
    void shouldDeleteCurrentUserApiKey() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/api-keys/key_1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldListCurrentUserLoginActivities() throws Exception {
        when(listCurrentUserLoginActivitiesUseCase.execute(0, 20))
                .thenReturn(new TerminalReadPort.LoginActivityPageSnapshot(
                        List.of(new TerminalReadPort.LoginActivitySnapshot(
                                LocalDateTime.of(2026, 4, 2, 10, 0),
                                "127.0.0.1",
                                "MarcusTerminal/2.0",
                                true
                        )),
                        new TerminalReadPort.OffsetPaginationMetaSnapshot(0, 20, 1, 1, false)
                ));

        mockMvc.perform(get("/api/v1/users/me/login-activities").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ipAddress").value("127.0.0.1"))
                .andExpect(jsonPath("$.meta.page").value(0));
    }
}
