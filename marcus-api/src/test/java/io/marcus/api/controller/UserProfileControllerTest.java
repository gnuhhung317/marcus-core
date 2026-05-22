package io.marcus.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.application.dto.UpdateUserProfileRequest;
import io.marcus.application.usecase.GetCurrentUserProfileUseCase;
import io.marcus.application.usecase.ListCurrentUserLoginActivitiesUseCase;
import io.marcus.application.usecase.UpdateCurrentUserProfileUseCase;
import io.marcus.domain.port.UserProfileReadPort;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private UpdateCurrentUserProfileUseCase updateCurrentUserProfileUseCase;

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
                .thenReturn(new UserProfileReadPort.UserProfileSnapshot("usr_1", "trader_1", "trader@marcus.local", "USER"));

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("usr_1"));
    }

    @Test
    void shouldUpdateCurrentUserProfile() throws Exception {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("trader_2", "trader2@marcus.local");
        when(updateCurrentUserProfileUseCase.execute(any(UpdateUserProfileRequest.class)))
                .thenReturn(new UserProfileReadPort.UserProfileSnapshot("usr_1", "trader_2", "trader2@marcus.local", "USER"));

        mockMvc.perform(put("/api/v1/users/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("trader_2"))
                .andExpect(jsonPath("$.email").value("trader2@marcus.local"));
    }

    @Test
    void shouldListCurrentUserLoginActivities() throws Exception {
        when(listCurrentUserLoginActivitiesUseCase.execute(0, 20))
                .thenReturn(new UserProfileReadPort.LoginActivityPageSnapshot(
                        List.of(new UserProfileReadPort.LoginActivitySnapshot(
                                LocalDateTime.of(2026, 4, 2, 10, 0),
                                "127.0.0.1",
                                "MarcusTerminal/2.0",
                                true
                        )),
                        new UserProfileReadPort.OffsetPaginationMetaSnapshot(0, 20, 1, 1, false)
                ));

        mockMvc.perform(get("/api/v1/users/me/login-activities").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].ipAddress").value("127.0.0.1"))
                .andExpect(jsonPath("$.meta.page").value(0));
    }
}
