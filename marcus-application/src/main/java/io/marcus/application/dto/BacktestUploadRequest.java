package io.marcus.application.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;

public record BacktestUploadRequest(
        String runName,
        @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class) LocalDateTime startedAt,
        @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class) LocalDateTime endedAt,
        Map<String, Object> metrics,
        @NotEmpty List<@Valid EquityPoint> equityHistory,
        @NotNull List<@Valid ClosedTrade> closedTrades
) {

    public static class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText().trim();
            if (text.isEmpty()) {
                return null;
            }
            try {
                return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                try {
                    return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                            .atZoneSameInstant(ZoneOffset.UTC)
                            .toLocalDateTime();
                } catch (Exception ex) {
                    throw new IOException("Failed to parse date-time string: " + text, ex);
                }
            }
        }
    }

    public record EquityPoint(
            @NotNull @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class) LocalDateTime timestamp,
            @NotNull BigDecimal cash,
            @NotNull BigDecimal equity,
            @NotNull BigDecimal realizedPnl,
            @NotNull BigDecimal unrealizedPnl,
            @NotNull BigDecimal totalFees
    ) {
    }

    public record ClosedTrade(
            @NotBlank String symbol,
            @NotBlank String marketType,
            @NotBlank String side,
            @NotNull BigDecimal quantity,
            @NotNull BigDecimal entryPrice,
            @NotNull BigDecimal exitPrice,
            @NotNull BigDecimal pnl,
            @NotNull BigDecimal fees,
            @NotNull @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class) LocalDateTime entryTimestamp,
            @NotNull @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class) LocalDateTime exitTimestamp,
            BigDecimal durationSeconds
    ) {
    }
}

