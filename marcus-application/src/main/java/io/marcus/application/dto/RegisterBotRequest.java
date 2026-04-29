package io.marcus.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record RegisterBotRequest(
        String description,
        String tradingPair, // TODO: should be enum
        String botName,
        @JsonAlias("exchange")
        String exchangeId
        ) {

}
