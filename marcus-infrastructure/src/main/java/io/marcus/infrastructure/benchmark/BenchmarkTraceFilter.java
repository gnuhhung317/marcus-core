package io.marcus.infrastructure.benchmark;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.marcus.infrastructure.security.filter.RequestCachingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Opt-in benchmark trace filter for signal ingestion.
 *
 * <p>When enabled, the filter captures a compact JSONL record for benchmark
 * runs and also echoes trace headers back to the caller so the load generator
 * can measure request completion without needing backend log access.
 */
public class BenchmarkTraceFilter extends OncePerRequestFilter {

    public static final String REQUEST_ATTRIBUTE = "marcus.benchmark.trace";
    public static final String HEADER_TRACE_ID = "X-Benchmark-Trace-Id";
    public static final String HEADER_RUN_ID = "X-Benchmark-Run-Id";
    public static final String HEADER_CASE_ID = "X-Benchmark-Case-Id";
    public static final String HEADER_WORKER_ID = "X-Benchmark-Worker-Id";
    public static final String HEADER_SIGNAL_ID = "X-Benchmark-Signal-Id";
    public static final String HEADER_RECEIVED_AT = "X-Benchmark-Received-At";
    public static final String HEADER_COMPLETED_AT = "X-Benchmark-Completed-At";
    public static final String HEADER_OUTCOME = "X-Benchmark-Outcome";
    public static final String HEADER_STATUS = "X-Benchmark-Status";

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Path traceFile;
    private final Object traceWriteLock = new Object();

    public BenchmarkTraceFilter(ObjectMapper objectMapper, boolean enabled, String traceFilePath) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.traceFile = StringUtils.hasText(traceFilePath) ? Path.of(traceFilePath) : null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !StringUtils.hasText(resolveRunId(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Instant receivedAt = Instant.now();
        BenchmarkTraceRecord record = buildRecord(request, receivedAt);
        request.setAttribute(REQUEST_ATTRIBUTE, record);

        try {
            filterChain.doFilter(request, response);
        } finally {
            Instant completedAt = Instant.now();
            BenchmarkTraceRecord completedRecord = record.complete(
                    completedAt,
                    response.getStatus(),
                    resolveOutcome(response.getStatus())
            );

            addResponseHeaders(response, completedRecord);
            writeTrace(completedRecord);
        }
    }

    private void addResponseHeaders(HttpServletResponse response, BenchmarkTraceRecord record) {
        response.setHeader(HEADER_TRACE_ID, record.traceId());
        if (StringUtils.hasText(record.runId())) {
            response.setHeader(HEADER_RUN_ID, record.runId());
        }
        if (StringUtils.hasText(record.caseId())) {
            response.setHeader(HEADER_CASE_ID, record.caseId());
        }
        if (StringUtils.hasText(record.workerId())) {
            response.setHeader(HEADER_WORKER_ID, record.workerId());
        }
        if (StringUtils.hasText(record.signalId())) {
            response.setHeader(HEADER_SIGNAL_ID, record.signalId());
        }
        response.setHeader(HEADER_RECEIVED_AT, record.receivedAt().toString());
        response.setHeader(HEADER_COMPLETED_AT, record.completedAt().toString());
        response.setHeader(HEADER_OUTCOME, record.outcome());
        response.setHeader(HEADER_STATUS, Integer.toString(record.status()));
    }

    private void writeTrace(BenchmarkTraceRecord record) {
        if (traceFile == null) {
            return;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("traceId", record.traceId());
        payload.put("runId", record.runId());
        payload.put("caseId", record.caseId());
        payload.put("workerId", record.workerId());
        payload.put("signalId", record.signalId());
        payload.put("method", record.method());
        payload.put("path", record.path());
        payload.put("receivedAt", record.receivedAt().toString());
        payload.put("completedAt", record.completedAt().toString());
        payload.put("durationMs", Duration.between(record.receivedAt(), record.completedAt()).toMillis());
        payload.put("status", record.status());
        payload.put("outcome", record.outcome());

        try {
            if (traceFile.getParent() != null) {
                Files.createDirectories(traceFile.getParent());
            }
            String line = objectMapper.writeValueAsString(payload) + System.lineSeparator();
            synchronized (traceWriteLock) {
                Files.writeString(
                        traceFile,
                        line,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            }
        } catch (Exception ignored) {
            // Trace writing must never break the actual request path.
        }
    }

    private String resolveRunId(HttpServletRequest request) {
        String runId = request.getHeader(HEADER_RUN_ID);
        if (StringUtils.hasText(runId)) {
            return runId.trim();
        }

        String body = RequestCachingFilter.currentRequestBody();
        if (!StringUtils.hasText(body)) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode metadata = root.path("metadata");
            if (metadata.isObject()) {
                String value = firstText(metadata, "benchmarkRunId", "benchmark_run_id", "runId", "run_id");
                if (StringUtils.hasText(value)) {
                    return value.trim();
                }
            }
            String rootValue = firstText(root, "benchmarkRunId", "benchmark_run_id", "runId", "run_id");
            return StringUtils.hasText(rootValue) ? rootValue.trim() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String firstText(JsonNode node, String... keys) {
        if (node == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (StringUtils.hasText(key) && node.has(key)) {
                String value = node.path(key).asText("");
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }
        return null;
    }

    private BenchmarkTraceRecord buildRecord(HttpServletRequest request, Instant receivedAt) {
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString();
        }

        String runId = request.getHeader(HEADER_RUN_ID);
        String caseId = request.getHeader(HEADER_CASE_ID);
        String workerId = request.getHeader(HEADER_WORKER_ID);
        String signalId = null;

        String body = RequestCachingFilter.currentRequestBody();
        if (StringUtils.hasText(body)) {
            try {
                JsonNode root = objectMapper.readTree(body);
                signalId = firstText(root, "signalId", "signal_id");
                JsonNode metadata = root.path("metadata");
                if (metadata.isObject()) {
                    runId = resolveWithFallback(runId, firstText(metadata, "benchmarkRunId", "benchmark_run_id", "runId", "run_id"));
                    caseId = resolveWithFallback(caseId, firstText(metadata, "benchmarkCaseId", "benchmark_case_id", "caseId", "case_id"));
                    workerId = resolveWithFallback(workerId, firstText(metadata, "benchmarkWorkerId", "benchmark_worker_id", "workerId", "worker_id"));
                }
            } catch (Exception ignored) {
                // Best-effort tracing only.
            }
        }

        return new BenchmarkTraceRecord(
                traceId,
                runId,
                caseId,
                workerId,
                signalId,
                request.getMethod(),
                request.getRequestURI(),
                receivedAt,
                receivedAt,
                0,
                "pending"
        );
    }

    private String resolveWithFallback(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary.trim();
        }
        if (StringUtils.hasText(fallback)) {
            return fallback.trim();
        }
        return null;
    }

    private String resolveOutcome(int status) {
        if (status >= 200 && status < 300) {
            return "success";
        }
        if (status == 409 || status == 422) {
            return "rejected";
        }
        return "error";
    }

    private record BenchmarkTraceRecord(
            String traceId,
            String runId,
            String caseId,
            String workerId,
            String signalId,
            String method,
            String path,
            Instant receivedAt,
            Instant completedAt,
            int status,
            String outcome
    ) {
        BenchmarkTraceRecord complete(Instant completedAt, int status, String outcome) {
            return new BenchmarkTraceRecord(
                    traceId,
                    runId,
                    caseId,
                    workerId,
                    signalId,
                    method,
                    path,
                    receivedAt,
                    completedAt,
                    status,
                    outcome
            );
        }
    }
}
