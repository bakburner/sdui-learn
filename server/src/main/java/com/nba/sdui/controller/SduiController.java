package com.nba.sdui.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nba.sdui.error.UnsupportedSectionException;
import com.nba.sdui.models.generated.Screen;
import com.nba.sdui.orchestration.ParameterizedRefreshService;
import com.nba.sdui.orchestration.ResponseMetaCollector;
import com.nba.sdui.orchestration.SduiCompositionService;
import com.nba.sdui.orchestration.SectionRefreshService;
import com.nba.sdui.request.SduiRequestContext;
import com.nba.sdui.versioning.SchemaVersion;
import com.nba.sdui.versioning.SchemaVersionChecker;
import com.nba.sdui.versioning.SchemaVersionConfig;
import com.nba.sdui.versioning.SchemaVersionFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller for SDUI endpoints.
 *
 * <p>All composition endpoints accept {@link SduiRequestContext}, resolved from
 * bracket-notation query params (GET) or JSON body (POST) by
 * {@link com.nba.sdui.request.BracketParamResolver}.
 *
 * <p>Headers retained: {@code Authorization} (auth). Correlation
 * ({@code X-Correlation-ID}) is owned by SAF's filter; legacy
 * {@code X-Trace-Id} is observed for the transition window only.
 * All other context travels as query parameters per plan-request-transport.md D3.
 */
@RestController
public class SduiController {

    private static final Logger log = LoggerFactory.getLogger(SduiController.class);

    private final SduiCompositionService compositionService;
    private final SectionRefreshService sectionRefreshService;
    private final ParameterizedRefreshService parameterizedRefreshService;
    private final ObjectMapper objectMapper;
    private final SchemaVersionChecker versionChecker;
    private final SchemaVersionConfig versionConfig;
    private final SchemaVersionFilter versionFilter;
    private final com.nba.sdui.metrics.SduiMetrics metrics;
    private final ObjectProvider<ResponseMetaCollector> metaCollector;

    public SduiController(SduiCompositionService compositionService,
                          SectionRefreshService sectionRefreshService,
                          ParameterizedRefreshService parameterizedRefreshService,
                          ObjectMapper objectMapper,
                          SchemaVersionChecker versionChecker,
                          SchemaVersionConfig versionConfig,
                          SchemaVersionFilter versionFilter,
                          com.nba.sdui.metrics.SduiMetrics metrics,
                          ObjectProvider<ResponseMetaCollector> metaCollector) {
        this.compositionService = compositionService;
        this.sectionRefreshService = sectionRefreshService;
        this.parameterizedRefreshService = parameterizedRefreshService;
        this.objectMapper = objectMapper;
        this.versionChecker = versionChecker;
        this.versionConfig = versionConfig;
        this.versionFilter = versionFilter;
        this.metrics = metrics;
        this.metaCollector = metaCollector;
    }

    // ── Game Detail ────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/game/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getGameDetail(
            @PathVariable String gameId,
            SduiRequestContext ctx,
            HttpServletResponse response) {


        log.info("SDUI request: gameId={}, locale={}, schemaVersion={}",
            gameId, ctx.getLocale(), ctx.getSchemaVersion());

        // Version gate: reject clients below minimum supported version
        ResponseEntity<Object> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            var result = compositionService.composeGameDetail(gameId, ctx);

            setResponseHeaders(response, ctx);
            log.info("SDUI response composed successfully");

            // Apply version-aware field stripping
            JsonNode filtered = applyVersionFilter(objectMapper.valueToTree(result.response()), ctx);

            // Cache-control based on server-derived game state
            CacheControl cache = resolveCacheControl(result.derivedGameState(), "pre");
            return ResponseEntity.ok().cacheControl(cache).body(envelope(filtered));

        } catch (Exception e) {
            log.error("Error composing SDUI response", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/game/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> postGameDetail(
            @PathVariable String gameId,
            SduiRequestContext ctx,
            HttpServletResponse response) {
        return getGameDetail(gameId, ctx, response);
    }

    // ── Scoreboard ─────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/scoreboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getScoreboard(
            SduiRequestContext ctx,
            HttpServletResponse response) {


        log.info("SDUI scoreboard request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        ResponseEntity<Object> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            Screen screenResponse = compositionService.composeScoreboard(ctx);
            setResponseHeaders(response, ctx);
            log.info("SDUI scoreboard response composed successfully");
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                    .body(envelope(applyVersionFilter(objectMapper.valueToTree(screenResponse), ctx)));
        } catch (Exception e) {
            log.error("Error composing SDUI scoreboard response", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/scoreboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> postScoreboard(SduiRequestContext ctx, HttpServletResponse response) {
        return getScoreboard(ctx, response);
    }

    // ── Stats (polling endpoint — not a composition endpoint) ──────────

    @GetMapping(value = "/v1/api/stats/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getStats(
            @PathVariable String gameId) {


        log.info("Stats request: gameId={}", gameId);

        try {
            SduiCompositionService.StatsResponse statsResponse = compositionService.getPlayerStats(gameId);
            log.info("Stats response composed successfully");
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .body(statsResponse);
        } catch (Exception e) {
            log.error("Error fetching stats", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ── For You ────────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/for-you", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getForYou(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        log.info("SDUI for-you request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        ResponseEntity<Object> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            Screen screenResponse = compositionService.composeForYou(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(120)).cachePrivate())
                    .body(envelope(applyVersionFilter(objectMapper.valueToTree(screenResponse), ctx)));
        } catch (Exception e) {
            log.error("Error composing for-you screen", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/for-you", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> postForYou(SduiRequestContext ctx, HttpServletResponse response) {
        return getForYou(ctx, response);
    }

    // ── Watch ──────────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/watch", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getWatch(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        log.info("SDUI watch request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        ResponseEntity<Object> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            Screen screenResponse = compositionService.composeWatch(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(120)).cachePublic())
                    .body(envelope(applyVersionFilter(objectMapper.valueToTree(screenResponse), ctx)));
        } catch (Exception e) {
            log.error("Error composing watch screen", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/watch", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> postWatch(SduiRequestContext ctx, HttpServletResponse response) {
        return getWatch(ctx, response);
    }

    // ── Games / Live ───────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/games", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getGames(
            @RequestParam Map<String, String> allParams,
            SduiRequestContext ctx,
            HttpServletResponse response) {


        Map<String, String> userParams = stripEnvelopeKeys(allParams);
        log.info("SDUI games request: locale={}, schemaVersion={}, userParams={}",
                ctx.getLocale(), ctx.getSchemaVersion(), userParams);

        ResponseEntity<Object> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            JsonNode screenResponse;
            if (!userParams.isEmpty()) {
                var resolved = parameterizedRefreshService.refreshScreen(
                        "games", ctx.getTraceId(), userParams, ctx);
                if (resolved.isEmpty()) {
                    return ResponseEntity.notFound().build();
                }
                screenResponse = objectMapper.valueToTree(resolved.get());
            } else {
                screenResponse = objectMapper.valueToTree(compositionService.composeLive(ctx));
            }
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .body(envelope(applyVersionFilter(screenResponse, ctx)));
        } catch (Exception e) {
            log.error("Error composing games screen", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/games", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> postGames(
            @RequestParam Map<String, String> allParams,
            SduiRequestContext ctx,
            HttpServletResponse response) {
        return getGames(allParams, ctx, response);
    }

    // ── Calendar ────────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/calendar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getCalendar(
            @RequestParam Map<String, String> allParams,
            SduiRequestContext ctx,
            HttpServletResponse response) {


        Map<String, String> userParams = stripEnvelopeKeys(allParams);
        log.info("SDUI calendar request: locale={}, schemaVersion={}, userParams={}",
                ctx.getLocale(), ctx.getSchemaVersion(), userParams);

        ResponseEntity<Object> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            Screen screenResponse = compositionService.composeCalendar(ctx, userParams.get("date"));
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .body(envelope(applyVersionFilter(objectMapper.valueToTree(screenResponse), ctx)));
        } catch (Exception e) {
            log.error("Error composing calendar screen", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/calendar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> postCalendar(
            @RequestParam Map<String, String> allParams,
            SduiRequestContext ctx,
            HttpServletResponse response) {
        return getCalendar(allParams, ctx, response);
    }

    // ── Schedule ───────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/schedule", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getSchedule(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        log.info("SDUI schedule request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        ResponseEntity<Object> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            Screen screenResponse = compositionService.composeSchedule(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(300)).cachePublic())
                    .body(envelope(applyVersionFilter(objectMapper.valueToTree(screenResponse), ctx)));
        } catch (Exception e) {
            log.error("Error composing schedule screen", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/schedule", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> postSchedule(SduiRequestContext ctx, HttpServletResponse response) {
        return getSchedule(ctx, response);
    }

    // ── Demos ──────────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/demos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getDemos(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        log.info("SDUI demos request: locale={}, schemaVersion={}",
            ctx.getLocale(), ctx.getSchemaVersion());

        ResponseEntity<Object> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            Screen screenResponse = compositionService.composeDemos(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                    .body(envelope(applyVersionFilter(objectMapper.valueToTree(screenResponse), ctx)));
        } catch (Exception e) {
            log.error("Error composing demos screen", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/demos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> postDemos(SduiRequestContext ctx, HttpServletResponse response) {
        return getDemos(ctx, response);
    }

    // ── Home (NBA.com style) ───────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/home", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getHome(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        log.info("SDUI home request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        ResponseEntity<Object> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            Screen screenResponse = compositionService.composeHome(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(120)).cachePublic())
                    .body(envelope(applyVersionFilter(objectMapper.valueToTree(screenResponse), ctx)));
        } catch (Exception e) {
            log.error("Error composing home screen", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/home", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> postHome(SduiRequestContext ctx, HttpServletResponse response) {
        return getHome(ctx, response);
    }

    // ── Leaders ────────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/leaders", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getLeaders(
            @RequestParam Map<String, String> allParams,
            SduiRequestContext ctx,
            HttpServletResponse response) {


        Map<String, String> userParams = stripEnvelopeKeys(allParams);
        log.info("SDUI leaders request: locale={}, schemaVersion={}, userParams={}",
            ctx.getLocale(), ctx.getSchemaVersion(), userParams);

        ResponseEntity<Object> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            JsonNode screenResponse;
            if (!userParams.isEmpty()) {
                var resolved = parameterizedRefreshService.refreshScreen(
                        "leaders", ctx.getTraceId(), userParams, ctx);
                if (resolved.isEmpty()) {
                    return ResponseEntity.notFound().build();
                }
                screenResponse = objectMapper.valueToTree(resolved.get());
            } else {
                screenResponse = objectMapper.valueToTree(compositionService.composeLeaders(ctx));
            }
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(300)).cachePublic())
                    .body(envelope(applyVersionFilter(screenResponse, ctx)));
        } catch (Exception e) {
            log.error("Error composing leaders screen", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/leaders", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> postLeaders(
            @RequestParam Map<String, String> allParams,
            SduiRequestContext ctx,
            HttpServletResponse response) {
        return getLeaders(allParams, ctx, response);
    }

    // ── Boxscore ───────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/boxscore/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getBoxscore(
            @PathVariable String gameId,
            SduiRequestContext ctx,
            HttpServletResponse response) {

        log.info("SDUI boxscore request: gameId={}, locale={}, schemaVersion={}", gameId, ctx.getLocale(), ctx.getSchemaVersion());

        ResponseEntity<Object> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            Screen screenResponse = compositionService.composeBoxscore(gameId, ctx);
            setResponseHeaders(response, ctx);
            log.info("SDUI boxscore response composed successfully");
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .body(envelope(applyVersionFilter(objectMapper.valueToTree(screenResponse), ctx)));
        } catch (Exception e) {
            log.error("Error composing SDUI boxscore response", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/boxscore/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> postBoxscore(
            @PathVariable String gameId, SduiRequestContext ctx, HttpServletResponse response) {
        return getBoxscore(gameId, ctx, response);
    }

    // ── Section Refresh ────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/section/{sectionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getSection(
            @PathVariable String sectionId,
            SduiRequestContext ctx,
            HttpServletResponse response) {

        log.info("SDUI section refresh request: sectionId={}, locale={}, schemaVersion={}",
                sectionId, ctx.getLocale(), ctx.getSchemaVersion());

        try {
            ResponseEntity<Object> mismatch = checkVersionMismatch(ctx, response);
            if (mismatch != null) return mismatch;

            var section = sectionRefreshService.refreshSection(sectionId, ctx);
            setResponseHeaders(response, ctx);
            if (section.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            JsonNode filtered = applyVersionFilter(section.get(), ctx);
            log.info("SDUI section refresh response composed: sectionId={}", sectionId);

            // Section refresh returns a single Section JSON object, not a screen envelope.
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .body(envelope(filtered));
        } catch (UnsupportedSectionException e) {
            log.warn("Unsupported section refresh for sectionId={}: {}", sectionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error composing SDUI section refresh response for sectionId={}", sectionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping(value = "/v1/sdui/section/{sectionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> postSection(
            @PathVariable String sectionId,
            SduiRequestContext ctx,
            HttpServletResponse response) {
        return getSection(sectionId, ctx, response);
    }

    /**
     * Strip envelope keys from a flat {@code @RequestParam} map so what remains
     * is the user-supplied filter params (the things a Form submitted). Envelope
     * keys are either bracket-notation ({@code platform[name]}, {@code device[zipCode]},
     * {@code experiments[exp_id]}) or top-level scalars the envelope already owns
     * ({@code locale}, {@code schemaVersion}).
     */
    private static Map<String, String> stripEnvelopeKeys(Map<String, String> params) {
        java.util.LinkedHashMap<String, String> filtered = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            String k = e.getKey();
            if (k.contains("[")) {
                continue;
            }
            if ("locale".equals(k) || "schemaVersion".equals(k)) {
                continue;
            }
            filtered.put(k, e.getValue());
        }
        return filtered;
    }

    // ── Init ──────────────────────────────────────────────────────────

    /**
     * Bootstrap endpoint — returns the initial navigation URI so clients
     * do not need to hardcode a starting screen.
     */
    @GetMapping(value = "/v1/sdui/screen/init", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> init(SduiRequestContext ctx, HttpServletResponse response) {
        setResponseHeaders(response, ctx);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("bootstrapUri", "nba://for-you");
        body.put("schemaVersion", ctx.getSchemaVersion());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(300)).cachePublic())
                .body(body);
    }

    @PostMapping(value = "/v1/sdui/screen/init", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> postInit(SduiRequestContext ctx, HttpServletResponse response) {
        return init(ctx, response);
    }

    // ── Health ──────────────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/healthz")
    public ResponseEntity<String> healthz() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/readyz")
    public ResponseEntity<String> readyz() {
        return ResponseEntity.ok("OK");
    }

    // ── Private helpers ────────────────────────────────────────────────


    /**
     * Set response headers from the request context. Correlation
     * ({@code X-Correlation-ID}) is owned by SAF's {@code CorrelationIdFilter}
     * and must not be written here.
     */
    private void setResponseHeaders(HttpServletResponse response, SduiRequestContext ctx) {
        response.setHeader("X-Schema-Version", ctx.getSchemaVersion());
    }

    /**
     * Wrap a composed payload in the standard {@link ResponseEnvelope}.
     * See AGENTS.md §1.2 "Transport-framing exception". {@code meta} is
     * built from the request-scoped {@link ResponseMetaCollector} so any
     * upstream failures or stale-if-error fragments observed during this
     * request are surfaced to the client.
     */
    private ResponseEnvelope<JsonNode> envelope(JsonNode payload) {
        ResponseMetaCollector collector = metaCollector == null ? null : metaCollector.getIfAvailable();
        ResponseMeta meta = collector == null ? ResponseMeta.fresh() : collector.build();
        return new ResponseEnvelope<>(payload, meta);
    }

    /**
     * Check if the client's schema version is below minimum supported.
     * If so, sets the mismatch header and returns the upgrade-required response.
     *
     * @return upgrade-required ResponseEntity, or null if client is supported
     */
    private ResponseEntity<Object> checkVersionMismatch(SduiRequestContext ctx, HttpServletResponse response) {
        if (versionChecker.isUpgradeRequired(ctx.getSchemaVersion())) {
            log.warn("Client schema version {} is below minimum supported {}",
                    ctx.getSchemaVersion(), versionConfig.getMinSupportedVersion());
            metrics.recordVersionMismatch(ctx.getSchemaVersion());
            response.setHeader(SchemaVersionChecker.MISMATCH_HEADER, SchemaVersionChecker.UPGRADE_REQUIRED);
            setResponseHeaders(response, ctx);
            JsonNode errorResponse = versionChecker.composeUpgradeRequiredResponse(
                    ctx.getSchemaVersion(), ctx.getTraceId());
            return ResponseEntity.ok().body(envelope(errorResponse));
        }
        return null;
    }

    /**
     * Apply version-aware field stripping to a composed response.
     * Strips fields and enum values that were introduced after the client's declared version.
     */
    private JsonNode applyVersionFilter(JsonNode composedResponse, SduiRequestContext ctx) {
        try {
            SchemaVersion clientVersion = SchemaVersion.parse(ctx.getSchemaVersion());
            return versionFilter.apply(composedResponse, clientVersion, versionConfig.currentVersion());
        } catch (IllegalArgumentException e) {
            log.warn("Cannot parse client schema version '{}', returning unfiltered response",
                    ctx.getSchemaVersion());
            return composedResponse;
        }
    }

    /**
     * Resolve Cache-Control based on game state (D7 mapping).
     */
    private CacheControl resolveCacheControl(String gameState, String defaultState) {
        String state = gameState != null ? gameState : defaultState;
        return switch (state) {
            case "live" -> CacheControl.noCache();
            case "post", "final" -> CacheControl.maxAge(Duration.ofSeconds(3600)).cachePublic();
            default -> CacheControl.maxAge(Duration.ofSeconds(300)).cachePublic(); // pre
        };
    }
}
