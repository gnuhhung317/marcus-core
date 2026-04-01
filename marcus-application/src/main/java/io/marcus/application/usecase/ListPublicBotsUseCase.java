package io.marcus.application.usecase;

import io.marcus.application.dto.BotSummaryResult;
import io.marcus.application.mapper.BotDtoMapper;
import io.marcus.domain.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListPublicBotsUseCase {

    private final BotRepository botRepository;
    private final BotDtoMapper botDtoMapper;

    public List<BotSummaryResult> execute() {
        return botRepository.findAllActive()
                .stream()
                .map(bot -> botDtoMapper.toSummaryResult(bot, false))
                .toList();
    }
}
