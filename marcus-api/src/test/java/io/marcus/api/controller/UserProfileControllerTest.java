package io.marcus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.config.SecurityConfig;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserProfileController.class)
@Import({GlobalExceptionsHandler.class, SecurityConfig.class})
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
                when(botSignatureInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        }

    @Test
    @WithMockUser(roles = "TRADER")
    void shouldUpdateCurrentUserPreferencesWhenAuthenticated() throws Exception {
        UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest("Asia/Ho_Chi_Minh", "vi-VN", true);
        TerminalReadPort.UserPreferencesSnapshot response = new TerminalReadPort.UserPreferencesSnapshot(
                "Asia/Ho_Chi_Minh",
                "vi-VN",
                true
        );
        when(updateCurrentUserPreferencesUseCase.execute(any(UpdateUserPreferencesRequest.class))).thenReturn(response);

        mockMvc.perform(put("/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("Asia/Ho_Chi_Minh"))
                .andExpect(jsonPath("$.locale").value("vi-VN"))
                .andExpect(jsonPath("$.emailNotificationsEnabled").value(true));

        verify(updateCurrentUserPreferencesUseCase).execute(request);
    }

    @Test
    @WithMockUser(roles = "TRADER")
    void shouldReturnBadRequestWhenUpdatePreferencesUseCaseThrowsIllegalArgument() throws Exception {
        UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest(null, "vi-VN", true);
        when(updateCurrentUserPreferencesUseCase.execute(any(UpdateUserPreferencesRequest.class)))
                .thenThrow(new IllegalArgumentException("Timezone is required"));

        mockMvc.perform(put("/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("Timezone is required"));
    }

    @Test
    @WithMockUser(roles = "TRADER")
    void shouldListCurrentUserApiKeysWhenAuthenticated() throws Exception {
        List<TerminalReadPort.ApiKeySummarySnapshot> response = List.of(
                new TerminalReadPort.ApiKeySummarySnapshot(
                        "key-1",
                        "Primary",
                        "mk_***_xyz",
                        LocalDateTime.of(2026, 4, 10, 10, 0),
                        LocalDateTime.of(2026, 4, 11, 11, 30)
                )
        );
        when(listCurrentUserApiKeysUseCase.execute()).thenReturn(response);

        mockMvc.perform(get("/users/me/api-keys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].apiKeyId").value("key-1"))
                .andExpect(jsonPath("$[0].label").value("Primary"))
                .andExpect(jsonPath("$[0].maskedKey").value("mk_***_xyz"));

        verify(listCurrentUserApiKeysUseCase).execute();
    }

    @Test
    @WithMockUser(roles = "TRADER")
    void shouldCreateCurrentUserApiKeyWhenAuthenticatedTraderRole() throws Exception {
        CreateApiKeyRequest request = new CreateApiKeyRequest("Primary key");
        TerminalReadPort.CreateApiKeySnapshot response = new TerminalReadPort.CreateApiKeySnapshot(
                "key-1",
                "mk_live_secret",
                "Primary key"
        );
        when(createCurrentUserApiKeyUseCase.execute(any(CreateApiKeyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/users/me/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.apiKeyId").value("key-1"))
                .andExpect(jsonPath("$.key").value("mk_live_secret"))
                .andExpect(jsonPath("$.label").value("Primary key"));

        verify(createCurrentUserApiKeyUseCase).execute(request);
    }

    @Test
    void shouldRejectCreateCurrentUserApiKeyWhenUnauthenticated() throws Exception {
        CreateApiKeyRequest request = new CreateApiKeyRequest("Primary key");

        mockMvc.perform(post("/users/me/api-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(createCurrentUserApiKeyUseCase, never()).execute(any(CreateApiKeyRequest.class));
    }

    @Test
    @WithMockUser(roles = "TRADER")
    void shouldDeleteCurrentUserApiKeyWhenAuthenticatedTraderRole() throws Exception {
        mockMvc.perform(delete("/users/me/api-keys/key-1"))
                .andExpect(status().isNoContent());

        verify(deleteCurrentUserApiKeyUseCase).execute("key-1");
    }

    @Test
    void shouldRejectDeleteCurrentUserApiKeyWhenUnauthenticated() throws Exception {
        mockMvc.perform(delete("/users/me/api-keys/key-1"))
                .andExpect(status().isForbidden());

        verify(deleteCurrentUserApiKeyUseCase, never()).execute(any(String.class));
    }

    @Test
    @WithMockUser(roles = "TRADER")
    void shouldListCurrentUserLoginActivitiesWithDefaultPaginationWhenNoQueryProvided() throws Exception {
        TerminalReadPort.LoginActivityPageSnapshot response = new TerminalReadPort.LoginActivityPageSnapshot(
                List.of(new TerminalReadPort.LoginActivitySnapshot(
                        LocalDateTime.of(2026, 4, 11, 12, 30),
                        "127.0.0.1",
                        "Mozilla",
                        true
                )),
                new TerminalReadPort.OffsetPaginationMetaSnapshot(0, 20, 1, 1, false)
        );
        when(listCurrentUserLoginActivitiesUseCase.execute(0, 20)).thenReturn(response);

        mockMvc.perform(get("/users/me/login-activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ipAddress").value("127.0.0.1"))
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.size").value(20));

        verify(listCurrentUserLoginActivitiesUseCase).execute(0, 20);
    }

    @Test
    @WithMockUser(roles = "TRADER")
    void shouldListCurrentUserLoginActivitiesWithProvidedPagination() throws Exception {
        TerminalReadPort.LoginActivityPageSnapshot response = new TerminalReadPort.LoginActivityPageSnapshot(
                List.of(),
                new TerminalReadPort.OffsetPaginationMetaSnapshot(1, 5, 0, 0, false)
        );
        when(listCurrentUserLoginActivitiesUseCase.execute(1, 5)).thenReturn(response);

        mockMvc.perform(get("/users/me/login-activities")
                        .queryParam("page", "1")
                        .queryParam("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.size").value(5));

        verify(listCurrentUserLoginActivitiesUseCase).execute(1, 5);
    }
}