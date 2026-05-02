package io.marcus.application.usecase;

<<<<<<< HEAD
import io.marcus.domain.port.TerminalReadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
=======
import io.marcus.application.dto.BotSummaryResult;
import io.marcus.application.mapper.BotDtoMapper;
import io.marcus.domain.repository.BotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
>>>>>>> 07cc74d5f615dfb2d511f1f2832e810f702e72e8

@Service
@RequiredArgsConstructor
public class ListPublicBotsUseCase {

<<<<<<< HEAD
    private static final Set<String> SUPPORTED_RISKS = Set.of("LOW", "MEDIUM", "HIGH", "ALL");
    private static final Set<String> SUPPORTED_SORTS = Set.of(
            "return",
            "-return",
            "drawdown",
            "-drawdown",
            "subscribers",
            "-subscribers"
    );

    private final TerminalReadPort terminalReadPort;

    public TerminalReadPort.BotDiscoveryPageSnapshot execute(
            String q,
            String asset,
            String risk,
            String sort,
            int page,
            int size
    ) {
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, 100));
        String normalizedQuery = normalizeText(q);
        String normalizedAsset = normalizeAsset(asset);
        String normalizedRisk = normalizeRisk(risk);
        String normalizedSort = normalizeSort(sort);

        return terminalReadPort.listPublicBots(
                normalizedQuery,
                normalizedAsset,
                normalizedRisk,
                normalizedSort,
                normalizedPage,
                normalizedSize
        );
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeAsset(String asset) {
        String normalizedAsset = normalizeText(asset);
        return normalizedAsset == null ? null : normalizedAsset.toUpperCase(Locale.ROOT);
    }

    private String normalizeRisk(String risk) {
        String normalizedRisk = risk == null || risk.isBlank()
                ? "ALL"
                : risk.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_RISKS.contains(normalizedRisk)) {
            throw new IllegalArgumentException("Unsupported risk: " + risk);
        }
        return normalizedRisk;
    }

    private String normalizeSort(String sort) {
        String normalizedSort = sort == null || sort.isBlank()
                ? "-return"
                : sort.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_SORTS.contains(normalizedSort)) {
            throw new IllegalArgumentException("Unsupported sort: " + sort);
        }
        return normalizedSort;
=======
    private final BotRepository botRepository;
    private final BotDtoMapper botDtoMapper;

    public List<BotSummaryResult> execute() {
        return botRepository.findAllActive()
                .stream()
                .map(bot -> botDtoMapper.toSummaryResult(bot, false))
                .toList();
>>>>>>> 07cc74d5f615dfb2d511f1f2832e810f702e72e8
    }
}
