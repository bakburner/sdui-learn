package com.nba.sdui.remote;

import com.fasterxml.jackson.databind.JsonNode;
import com.nba.saf.dto.ServiceMetadata;
import com.nba.saf.orchestrator.OrchestratorFactory;
import com.nba.saf.orchestrator.ServiceCall;
import com.nba.saf.orchestrator.ServiceOrchestrator;
import com.nba.sdui.domain.port.ScoreboardPort;
import com.nba.sdui.domain.port.StatsPort;
import com.nba.sdui.metrics.SduiMetrics;
import com.nba.sdui.orchestration.ResponseMetaCollector;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Adapter wiring the domain ports to {@link StatsApiClient} via the SAF
 * {@link OrchestratorFactory}. Each upstream call goes through a one-shot
 * {@link ServiceOrchestrator} so per-service resilience, two-tier caching,
 * request collapsing, and SLO tracking (configured under
 * {@code saf.services.<serviceName>}) wrap every fetch uniformly.
 *
 * <p>Per-call {@link ServiceMetadata} is consumed after each run to emit
 * the contract §10.1 {@code sdui.upstream.duration} timers and
 * {@code sdui.cache.hit/miss{layer=upstream}} counters.
 */
@Component
public class StatsApiAdapter implements ScoreboardPort, StatsPort {

    private static final Logger log = LoggerFactory.getLogger(StatsApiAdapter.class);

    /** Per-call timeout budget; matches the contract §8.1 declaration for live data. */
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(5);

    private static final String SERVICE_SCOREBOARD_CDN = "scoreboard-cdn";
    private static final String SERVICE_CORE_API = "core-api";
    private static final String SERVICE_BOXSCORE_CDN = "boxscore-cdn";
    private static final String SERVICE_SEASON_SCHEDULE = "season-schedule";

    private final StatsApiClient client;
    private final OrchestratorFactory orchestrators;
    private final SduiMetrics metrics;
    private final ObjectProvider<ResponseMetaCollector> metaCollector;

    public StatsApiAdapter(StatsApiClient client,
                           OrchestratorFactory orchestrators,
                           SduiMetrics metrics,
                           ObjectProvider<ResponseMetaCollector> metaCollector) {
        this.client = client;
        this.orchestrators = orchestrators;
        this.metrics = metrics;
        this.metaCollector = metaCollector;
    }

    /**
     * Test-only constructor: bypasses SAF orchestration when tests construct the
     * adapter without a Spring context. Production code always wires the
     * four-arg constructor via {@code @Component} injection.
     */
    public StatsApiAdapter(StatsApiClient client) {
        this(client, null, null, null);
    }

    @Override
    public JsonNode getScoreboard() throws IOException {
        return execute(
                "scoreboard",
                SERVICE_SCOREBOARD_CDN,
                "scoreboard:cdn:today",
                () -> uncheck(client::getScoreboard));
    }

    @Override
    public JsonNode getScoreboardForDate(LocalDate date) throws IOException {
        return execute(
                "scoreboard:" + date,
                SERVICE_CORE_API,
                "scoreboard:core:" + date,
                () -> uncheck(() -> client.getScoreboardForDate(date)));
    }

    @Override
    public JsonNode getBoxscore(String gameId) throws IOException {
        return execute(
                "boxscore:" + gameId,
                SERVICE_BOXSCORE_CDN,
                "boxscore:" + gameId,
                () -> uncheck(() -> client.getBoxscore(gameId)));
    }

    @Override
    public Map<LocalDate, Integer> getSeasonGameCounts() throws IOException {
        return execute(
                "season-schedule",
                SERVICE_SEASON_SCHEDULE,
                "season-schedule:current",
                () -> uncheck(client::getSeasonGameCounts));
    }

    private <T> T execute(String id, String serviceName, String cacheKey, java.util.function.Supplier<T> call)
            throws IOException {
        if (orchestrators == null) {
            try {
                return call.get();
            } catch (UncheckedIOException uio) {
                throw uio.getCause();
            }
        }
        ServiceOrchestrator orchestrator = orchestrators.create(CALL_TIMEOUT);
        orchestrator.addService(ServiceCall.<T>builder()
                .id(id)
                .serviceName(serviceName)
                .call(call)
                .cache(cacheKey)
                .failOnError(true)
                .build());
        try {
            orchestrator.executeAll();
        } catch (RuntimeException e) {
            consumeMetadata(orchestrator, serviceName);
            Throwable cause = e.getCause();
            if (cause instanceof UncheckedIOException uio) {
                throw uio.getCause();
            }
            if (cause instanceof IOException io) {
                throw io;
            }
            throw e;
        }

        consumeMetadata(orchestrator, serviceName);

        Optional<T> result = orchestrator.<T>getResult(id)
                .map(r -> r.getData());
        return result.orElse(null);
    }

    private void consumeMetadata(ServiceOrchestrator orchestrator, String serviceName) {
        try {
            ServiceMetadata meta = orchestrator.buildMetadata();
            if (meta == null) return;
            ResponseMetaCollector collector = metaCollector == null ? null : metaCollector.getIfAvailable();
            if (collector != null) {
                collector.record(meta);
            }
            if (meta.getServices() == null) return;
            meta.getServices().values().stream()
                    .filter(info -> serviceName.equals(info.getServiceName()))
                    .forEach(info -> emit(info, serviceName));
        } catch (RuntimeException e) {
            log.debug("Skipped upstream metric emit for serviceName={}: {}", serviceName, e.getMessage());
        }
    }

    private void emit(ServiceMetadata.ServiceCallInfo info, String serviceName) {
        if (metrics == null) return;
        long latencyMs = info.getLatencyMs() != null ? info.getLatencyMs() : 0L;
        String status = info.getStatus() != null ? info.getStatus().name() : "UNKNOWN";
        metrics.recordUpstream(serviceName, status, Duration.ofMillis(latencyMs));

        Boolean cached = info.getCached();
        if (cached != null) {
            if (cached) {
                metrics.recordCacheHit("upstream", serviceName);
            } else {
                metrics.recordCacheMiss("upstream", serviceName);
            }
        }
    }

    @FunctionalInterface
    private interface IoSupplier<T> {
        T get() throws IOException;
    }

    private static <T> T uncheck(IoSupplier<T> s) {
        try {
            return s.get();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
