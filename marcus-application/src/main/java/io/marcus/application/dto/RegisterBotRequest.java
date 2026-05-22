package io.marcus.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;

public record RegisterBotRequest(
        @NotBlank(message = "Description is required")
        String description,

        @NotBlank(message = "Trading pair is required")
        String tradingPair, // TODO: should be enum

        @NotBlank(message = "Bot name is required")
        String botName,

        @NotBlank(message = "Exchange id is required")
        @JsonAlias("exchange")
        String exchangeId
) {

}
