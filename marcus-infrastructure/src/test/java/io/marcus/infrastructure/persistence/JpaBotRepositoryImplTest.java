package io.marcus.infrastructure.persistence;

import io.marcus.domain.model.Bot;
import io.marcus.domain.vo.BotStatus;
import io.marcus.infrastructure.persistence.entity.BotEntity;
import io.marcus.infrastructure.persistence.entity.ExchangeEntity;
import io.marcus.infrastructure.persistence.mapper.BotMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaBotRepositoryImplTest {

    @Mock
    private SpringDataBotRepository springDataBotRepository;

    @Mock
    private SpringDataExchangeRepository springDataExchangeRepository;

    @Mock
    private BotMapper botMapper;

    private JpaBotRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new JpaBotRepositoryImpl(springDataBotRepository, springDataExchangeRepository, botMapper);
    }

    @Test
    void shouldSaveBotWhenExchangeExists() {
        Bot bot = Bot.builder().botId("bot_1").exchangeId("binance").build();
        BotEntity mappedEntity = BotEntity.builder().botId("bot_1").build();
        ExchangeEntity exchange = ExchangeEntity.builder().exchangeId("binance").build();
        BotEntity savedEntity = BotEntity.builder().botId("bot_1").exchange(exchange).build();
        Bot expected = Bot.builder().botId("bot_1").exchangeId("binance").build();

        when(botMapper.toEntity(bot)).thenReturn(mappedEntity);
        when(springDataExchangeRepository.findByExchangeId("binance")).thenReturn(Optional.of(exchange));
        when(springDataBotRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(botMapper.toDomain(savedEntity)).thenReturn(expected);

        Bot actual = repository.save(bot);

        assertThat(actual).isEqualTo(expected);
        ArgumentCaptor<BotEntity> entityCaptor = ArgumentCaptor.forClass(BotEntity.class);
        verify(springDataBotRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getExchange()).isEqualTo(exchange);
    }

    @Test
    void shouldThrowWhenSavingBotWithUnknownExchange() {
        Bot bot = Bot.builder().botId("bot_1").exchangeId("unknown").build();
        BotEntity mappedEntity = BotEntity.builder().botId("bot_1").build();

        when(botMapper.toEntity(bot)).thenReturn(mappedEntity);
        when(springDataExchangeRepository.findByExchangeId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repository.save(bot))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Exchange not found: unknown");

        verify(springDataBotRepository, never()).save(any(BotEntity.class));
    }

    @Test
    void shouldFindBotByBotId() {
        BotEntity entity = BotEntity.builder().botId("bot_1").build();
        Bot domain = Bot.builder().botId("bot_1").build();

        when(springDataBotRepository.findByBotId("bot_1")).thenReturn(Optional.of(entity));
        when(botMapper.toDomain(entity)).thenReturn(domain);

        Optional<Bot> actual = repository.findByBotId("bot_1");

        assertThat(actual).contains(domain);
        verify(springDataBotRepository).findByBotId("bot_1");
    }

    @Test
    void shouldFindSecretByBotId() {
        BotEntity entity = BotEntity.builder().botId("bot_1").secretKey("secret").build();
        when(springDataBotRepository.findByBotId(eq("bot_1"))).thenReturn(Optional.of(entity));

        Optional<String> actual = repository.findSecretByBotId("bot_1");

        assertThat(actual).contains("secret");
    }

    @Test
    void shouldFindSecretByApiKey() {
        BotEntity entity = BotEntity.builder().apiKey("ak_1").secretKey("secret").build();
        when(springDataBotRepository.findByApiKey(eq("ak_1"))).thenReturn(Optional.of(entity));

        Optional<String> actual = repository.findSecretByApiKey("ak_1");

        assertThat(actual).contains("secret");
    }

    @Test
    void shouldFindAllActiveBots() {
        BotEntity entity = BotEntity.builder().botId("bot_1").status(BotStatus.ACTIVE).build();
        Bot domain = Bot.builder().botId("bot_1").status(BotStatus.ACTIVE).build();

        when(springDataBotRepository.findByStatus(BotStatus.ACTIVE)).thenReturn(List.of(entity));
        when(botMapper.toDomain(entity)).thenReturn(domain);

        List<Bot> actual = repository.findAllActive();

        assertThat(actual).containsExactly(domain);
        verify(springDataBotRepository).findByStatus(BotStatus.ACTIVE);
    }

    @Test
    void shouldFindAllByDeveloperId() {
        BotEntity entity = BotEntity.builder().botId("bot_1").developerId("dev_1").build();
        Bot domain = Bot.builder().botId("bot_1").developerId("dev_1").build();

        when(springDataBotRepository.findByDeveloperId("dev_1")).thenReturn(List.of(entity));
        when(botMapper.toDomain(entity)).thenReturn(domain);

        List<Bot> actual = repository.findAllByDeveloperId("dev_1");

        assertThat(actual).containsExactly(domain);
        verify(springDataBotRepository).findByDeveloperId("dev_1");
    }
}
