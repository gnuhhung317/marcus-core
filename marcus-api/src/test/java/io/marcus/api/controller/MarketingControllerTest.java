package io.marcus.api.controller;

import io.marcus.api.exception.GlobalExceptionsHandler;
import io.marcus.api.security.JwtAuthenticationFilter;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.ExchangeRepository;
import io.marcus.domain.repository.UserRepository;
import io.marcus.infrastructure.security.BotSignatureInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarketingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionsHandler.class)
class MarketingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private BotSignatureInterceptor botSignatureInterceptor;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private BotRepository botRepository;

    @MockBean
    private ExchangeRepository exchangeRepository;

    @Test
    void shouldGetMarketingStatsFromVersionedApiPrefix() throws Exception {
        when(userRepository.count()).thenReturn(12L);
        when(botRepository.countActive()).thenReturn(7L);
        when(exchangeRepository.count()).thenReturn(4L);

        mockMvc.perform(get("/api/v1/public/marketing/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verifiedDevelopers").value(62))
                .andExpect(jsonPath("$.activeCloudExecutors").value(7))
                .andExpect(jsonPath("$.systemUptime").value("24/7"))
                .andExpect(jsonPath("$.supportedExchanges").value(4));
    }
}
