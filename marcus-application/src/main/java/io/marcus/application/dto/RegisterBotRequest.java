package io.marcus.application.dto;


public record RegisterBotRequest (
    String developerId,
    String description,
    String tradingPair, // TODO: should be enum
    String botName,
    String exchangeId
){}
