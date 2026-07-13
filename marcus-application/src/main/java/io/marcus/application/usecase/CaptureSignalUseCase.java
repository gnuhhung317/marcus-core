package io.marcus.application.usecase;

import io.marcus.application.dto.CaptureSignalRequest;
import io.marcus.domain.exception.BotNotFoundException;
import io.marcus.domain.exception.DuplicateSignalException;
import io.marcus.domain.model.Bot;
import io.marcus.domain.model.Signal;
import io.marcus.domain.vo.BotStatus;
import io.marcus.domain.vo.MarginMode;
import io.marcus.domain.vo.MarketType;
import io.marcus.domain.vo.OrderType;
import io.marcus.domain.vo.SignalStatus;
import io.marcus.domain.port.SignalPublisherPort;
import io.marcus.domain.repository.BotRepository;
import io.marcus.domain.repository.SignalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CaptureSignalUseCase {

    private final SignalRepository signalRepository;
    private final BotRepository botRepository;
    private final SignalPublisherPort signalPublisherPort;

    public void execute(CaptureSignalRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Signal request is required");
        }

        // --- Guard: bot must exist and accept live signals ---
        Bot bot = botRepository.findByBotId(request.botId())
                .orElseThrow(() -> new BotNotFoundException("Bot not found: " + request.botId()));

        if (bot.getStatus() != BotStatus.ACTIVE) {
            throw new IllegalStateException("Only active bot can publish signals");
        }

        // --- Guard: idempotency — reject duplicate signalId ---
        if (signalRepository.existsBySignalId(request.signalId())) {
            throw new DuplicateSignalException(request.signalId());
        }

        // --- Build domain object without applying implicit defaults ---
        OrderType orderType = request.orderType();
        Signal signal = new Signal();
        signal.setSignalId(request.signalId());
        signal.setBotId(request.botId());
        signal.setSymbol(request.symbol());
        signal.setAction(request.action());
        signal.setMarketType(request.marketType());
        signal.setOrderType(orderType);
        signal.setEntry(request.entry());
        signal.setStopLoss(request.stopLoss());
        signal.setTakeProfit(request.takeProfit());
        signal.setAmount(request.amount());
        signal.setLeverage(request.leverage());
        signal.setMarginMode(request.marginMode());
        signal.setReduceOnly(request.reduceOnly());
        signal.setStatus(request.status() != null ? request.status() : SignalStatus.RECEIVED);
        signal.setGeneratedTimestamp(request.generatedTimestamp());
        signal.setTimeframe(request.timeframe());
        signal.setMetadata(request.metadata());
        signal.setPolicies(request.policies());

        // 1. Persist to PostgreSQL first
        signalRepository.save(signal);
        log.info("[Signal] Captured signalId={} botId={} action={} marketType={} orderType={} (SIMULATION={})",
                signal.getSignalId(), signal.getBotId(), signal.getAction(),
                signal.getMarketType(), signal.getOrderType(), signal.simulated());

        if (signal.simulated()) {
            log.info("[Signal] Skipping Kafka publication for simulated signalId={}", signal.getSignalId());
            return;
        }

        // 2. Publish to Kafka for global broadcast
        signalPublisherPort.publish(signal);
    }
}
