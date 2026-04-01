package io.marcus.infrastructure.persistence.mapper;

import io.marcus.domain.model.Signal;
import io.marcus.domain.vo.SignalAction;
import io.marcus.domain.vo.SignalStatus;
import io.marcus.infrastructure.persistence.entity.SignalEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SignalMapperTest {

    private final SignalMapper signalMapper = new SignalMapper();

    @Test
    void shouldMapMetadataWhenConvertingToEntity() {
        Signal signal = Signal.builder()
                .signalId("sig_1")
                .botId("bot_1")
                .action(SignalAction.OPEN_LONG)
                .status(SignalStatus.RECEIVED)
                .entry(new BigDecimal("123.45"))
                .metadata(Map.of("source", "bot", "confidence", 0.92))
                .build();

        SignalEntity entity = signalMapper.toEntity(signal);

        assertThat(entity).isNotNull();
        assertThat(entity.getMetadata()).containsEntry("source", "bot");
        assertThat(entity.getMetadata()).containsEntry("confidence", 0.92);
    }

    @Test
    void shouldMapMetadataWhenConvertingToDomain() {
        SignalEntity entity = SignalEntity.builder()
                .signalId("sig_1")
                .botId("bot_1")
                .action(SignalAction.CLOSE_LONG)
                .status(SignalStatus.RECEIVED)
                .metadata(Map.of("note", "partial", "retries", 2))
                .build();

        Signal signal = signalMapper.toDomain(entity);

        assertThat(signal).isNotNull();
        assertThat(signal.getMetadata()).containsEntry("note", "partial");
        assertThat(signal.getMetadata()).containsEntry("retries", 2);
    }
}
