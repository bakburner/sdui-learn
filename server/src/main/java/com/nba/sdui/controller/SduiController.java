package com.nba.sdui.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.nba.sdui.request.SduiRequestContext;
import com.nba.sdui.service.ParameterizedRefreshService;
import com.nba.sdui.service.SectionRefreshService;
import com.nba.sdui.service.SduiCompositionService;
import com.nba.sdui.service.UnsupportedSectionException;
import com.nba.sdui.versioning.SchemaVersion;
import com.nba.sdui.versioning.SchemaVersionChecker;
import com.nba.sdui.versioning.SchemaVersionConfig;
import com.nba.sdui.versioning.SchemaVersionFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for SDUI endpoints.
 *
 * <p>All composition endpoints accept {@link SduiRequestContext}, resolved from
 * bracket-notation query params (GET) or JSON body (POST) by
 * {@link com.nba.sdui.request.BracketParamResolver}.
 *
 * <p>Headers retained: {@code Authorization} (auth), {@code X-Trace-Id} (observability).
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

    public SduiController(SduiCompositionService compositionService,
                          SectionRefreshService sectionRefreshService,
                          ParameterizedRefreshService parameterizedRefreshService,
                          ObjectMapper objectMapper,
                          SchemaVersionChecker versionChecker,
                          SchemaVersionConfig versionConfig,
                          SchemaVersionFilter versionFilter) {
        this.compositionService = compositionService;
        this.sectionRefreshService = sectionRefreshService;
        this.parameterizedRefreshService = parameterizedRefreshService;
        this.objectMapper = objectMapper;
        this.versionChecker = versionChecker;
        this.versionConfig = versionConfig;
        this.versionFilter = versionFilter;
    }

    // ── Game Detail ────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/game/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getGameDetail(
            @PathVariable String gameId,
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());

        log.info("SDUI request: gameId={}, locale={}, schemaVersion={}",
            gameId, ctx.getLocale(), ctx.getSchemaVersion());

        // Version gate: reject clients below minimum supported version
        ResponseEntity<JsonNode> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            var result = compositionService.composeGameDetail(gameId, ctx);

            setResponseHeaders(response, ctx);
            log.info("SDUI response composed successfully");

            // Apply version-aware field stripping
            JsonNode filtered = applyVersionFilter(result.response(), ctx);

            // Cache-control based on server-derived game state
            CacheControl cache = resolveCacheControl(result.derivedGameState(), "pre");
            return ResponseEntity.ok().cacheControl(cache).body(filtered);

        } catch (Exception e) {
            log.error("Error composing SDUI response", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/game/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postGameDetail(
            @PathVariable String gameId,
            SduiRequestContext ctx,
            HttpServletResponse response) {
        return getGameDetail(gameId, ctx, response);
    }

    // ── Scoreboard ─────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/scoreboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getScoreboard(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());

        log.info("SDUI scoreboard request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        ResponseEntity<JsonNode> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            JsonNode screenResponse = compositionService.composeScoreboard(ctx);
            setResponseHeaders(response, ctx);
            log.info("SDUI scoreboard response composed successfully");
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                    .body(applyVersionFilter(screenResponse, ctx));
        } catch (Exception e) {
            log.error("Error composing SDUI scoreboard response", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/scoreboard", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postScoreboard(SduiRequestContext ctx, HttpServletResponse response) {
        return getScoreboard(ctx, response);
    }

    // ── Stats (polling endpoint — not a composition endpoint) ──────────

    @GetMapping(value = "/v1/api/stats/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getStats(
            @PathVariable String gameId) {

        String traceId = "trace-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);

        log.info("Stats request: gameId={}", gameId);

        try {
            JsonNode statsResponse = compositionService.getPlayerStats(gameId);
            log.info("Stats response composed successfully");
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .body(statsResponse);
        } catch (Exception e) {
            log.error("Error fetching stats", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    // ── For You ────────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/for-you", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getForYou(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());
        log.info("SDUI for-you request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        ResponseEntity<JsonNode> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            JsonNode screenResponse = compositionService.composeForYou(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(120)).cachePrivate())
                    .body(applyVersionFilter(screenResponse, ctx));
        } catch (Exception e) {
            log.error("Error composing for-you screen", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/for-you", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postForYou(SduiRequestContext ctx, HttpServletResponse response) {
        return getForYou(ctx, response);
    }

    // ── Watch ──────────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/watch", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getWatch(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());
        log.info("SDUI watch request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        ResponseEntity<JsonNode> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            JsonNode screenResponse = compositionService.composeWatch(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(120)).cachePublic())
                    .body(applyVersionFilter(screenResponse, ctx));
        } catch (Exception e) {
            log.error("Error composing watch screen", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/watch", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postWatch(SduiRequestContext ctx, HttpServletResponse response) {
        return getWatch(ctx, response);
    }

    // ── Games / Live ───────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/games", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getGames(
            @RequestParam Map<String, String> allParams,
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());

        Map<String, String> userParams = stripEnvelopeKeys(allParams);
        log.info("SDUI games request: locale={}, schemaVersion={}, userParams={}",
                ctx.getLocale(), ctx.getSchemaVersion(), userParams);

        ResponseEntity<JsonNode> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            JsonNode screenResponse;
            if (!userParams.isEmpty()) {
                var resolved = parameterizedRefreshService.refreshScreen(
                        "games", ctx.getTraceId(), userParams, ctx);
                if (resolved.isEmpty()) {
                    return ResponseEntity.notFound().build();
                }
                screenResponse = resolved.get();
            } else {
                screenResponse = compositionService.composeLive(ctx);
            }
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .body(applyVersionFilter(screenResponse, ctx));
        } catch (Exception e) {
            log.error("Error composing games screen", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/games", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postGames(
            @RequestParam Map<String, String> allParams,
            SduiRequestContext ctx,
            HttpServletResponse response) {
        return getGames(allParams, ctx, response);
    }

    // ── Schedule ───────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/schedule", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getSchedule(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());
        log.info("SDUI schedule request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        ResponseEntity<JsonNode> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            JsonNode screenResponse = compositionService.composeSchedule(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(300)).cachePublic())
                    .body(applyVersionFilter(screenResponse, ctx));
        } catch (Exception e) {
            log.error("Error composing schedule screen", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/schedule", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postSchedule(SduiRequestContext ctx, HttpServletResponse response) {
        return getSchedule(ctx, response);
    }

    // ── Demos ──────────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/demos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getDemos(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());
        log.info("SDUI demos request: locale={}, schemaVersion={}",
            ctx.getLocale(), ctx.getSchemaVersion());

        ResponseEntity<JsonNode> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            JsonNode screenResponse = compositionService.composeDemos(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic())
                    .body(applyVersionFilter(screenResponse, ctx));
        } catch (Exception e) {
            log.error("Error composing demos screen", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/demos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postDemos(SduiRequestContext ctx, HttpServletResponse response) {
        return getDemos(ctx, response);
    }

    // ── Home (NBA.com style) ───────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/home", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getHome(
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());
        log.info("SDUI home request: locale={}, schemaVersion={}", ctx.getLocale(), ctx.getSchemaVersion());

        ResponseEntity<JsonNode> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            JsonNode screenResponse = compositionService.composeHome(ctx);
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(120)).cachePublic())
                    .body(applyVersionFilter(screenResponse, ctx));
        } catch (Exception e) {
            log.error("Error composing home screen", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/home", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postHome(SduiRequestContext ctx, HttpServletResponse response) {
        return getHome(ctx, response);
    }

    // ── Leaders ────────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/leaders", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getLeaders(
            @RequestParam Map<String, String> allParams,
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());

        Map<String, String> userParams = stripEnvelopeKeys(allParams);
        log.info("SDUI leaders request: locale={}, schemaVersion={}, userParams={}",
            ctx.getLocale(), ctx.getSchemaVersion(), userParams);

        ResponseEntity<JsonNode> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            JsonNode screenResponse;
            if (!userParams.isEmpty()) {
                var resolved = parameterizedRefreshService.refreshScreen(
                        "leaders", ctx.getTraceId(), userParams, ctx);
                if (resolved.isEmpty()) {
                    return ResponseEntity.notFound().build();
                }
                screenResponse = resolved.get();
            } else {
                screenResponse = compositionService.composeLeaders(ctx);
            }
            setResponseHeaders(response, ctx);
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(Duration.ofSeconds(300)).cachePublic())
                    .body(applyVersionFilter(screenResponse, ctx));
        } catch (Exception e) {
            log.error("Error composing leaders screen", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/leaders", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postLeaders(
            @RequestParam Map<String, String> allParams,
            SduiRequestContext ctx,
            HttpServletResponse response) {
        return getLeaders(allParams, ctx, response);
    }

    // ── Boxscore ───────────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/screen/boxscore/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getBoxscore(
            @PathVariable String gameId,
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());
        log.info("SDUI boxscore request: gameId={}, locale={}, schemaVersion={}", gameId, ctx.getLocale(), ctx.getSchemaVersion());

        ResponseEntity<JsonNode> mismatch = checkVersionMismatch(ctx, response);
        if (mismatch != null) return mismatch;

        try {
            JsonNode screenResponse = compositionService.composeBoxscore(gameId, ctx);
            setResponseHeaders(response, ctx);
            log.info("SDUI boxscore response composed successfully");
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noCache())
                    .body(applyVersionFilter(screenResponse, ctx));
        } catch (Exception e) {
            log.error("Error composing SDUI boxscore response", e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/v1/sdui/screen/boxscore/{gameId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postBoxscore(
            @PathVariable String gameId, SduiRequestContext ctx, HttpServletResponse response) {
        return getBoxscore(gameId, ctx, response);
    }

    // ── Section Refresh ────────────────────────────────────────────────

    @GetMapping(value = "/v1/sdui/section/{sectionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> getSection(
            @PathVariable String sectionId,
            SduiRequestContext ctx,
            HttpServletResponse response) {

        ensureTraceId(ctx);
        MDC.put("traceId", ctx.getTraceId());
        log.info("SDUI section refresh request: sectionId={}, locale={}, schemaVersion={}",
                sectionId, ctx.getLocale(), ctx.getSchemaVersion());

        try {
            ResponseEntity<JsonNode> mismatch = checkVersionMismatch(ctx, response);
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
                    .body(filtered);
        } catch (UnsupportedSectionException e) {
            log.warn("Unsupported section refresh for sectionId={}: {}", sectionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error composing SDUI section refresh response for sectionId={}", sectionId, e);
            return ResponseEntity.internalServerError().build();
        } finally {
            MDC.clear();
        }
    }

    @PostMapping(value = "/v1/sdui/section/{sectionId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postSection(
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
    public ResponseEntity<JsonNode> init(SduiRequestContext ctx, HttpServletResponse response) {
        ensureTraceId(ctx);
        setResponseHeaders(response, ctx);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("bootstrapUri", "nba://for-you");
        body.put("schemaVersion", ctx.getSchemaVersion());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(300)).cachePublic())
                .body(body);
    }

    @PostMapping(value = "/v1/sdui/screen/init", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<JsonNode> postInit(SduiRequestContext ctx, HttpServletResponse response) {
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
     * Ensure the request context has a trace ID.
     * If the client didn't send X-Trace-Id, generate one.
     */
    private void ensureTraceId(SduiRequestContext ctx) {
        if (ctx.getTraceId() == null || ctx.getTraceId().isBlank()) {
            ctx.setTraceId("trace-" + UUID.randomUUID().toString().substring(0, 8));
        }
    }

    /** Set standard response headers from the request context. */
    private void setResponseHeaders(HttpServletResponse response, SduiRequestContext ctx) {
        response.setHeader("X-Trace-Id", ctx.getTraceId());
        response.setHeader("X-Schema-Version", ctx.getSchemaVersion());
    }

    /**
     * Check if the client's schema version is below minimum supported.
     * If so, sets the mismatch header and returns the upgrade-required response.
     *
     * @return upgrade-required ResponseEntity, or null if client is supported
     */
    private ResponseEntity<JsonNode> checkVersionMismatch(SduiRequestContext ctx, HttpServletResponse response) {
        if (versionChecker.isUpgradeRequired(ctx.getSchemaVersion())) {
            log.warn("Client schema version {} is below minimum supported {}",
                    ctx.getSchemaVersion(), versionConfig.getMinSupportedVersion());
            response.setHeader(SchemaVersionChecker.MISMATCH_HEADER, SchemaVersionChecker.UPGRADE_REQUIRED);
            setResponseHeaders(response, ctx);
            JsonNode errorResponse = versionChecker.composeUpgradeRequiredResponse(
                    ctx.getSchemaVersion(), ctx.getTraceId());
            return ResponseEntity.ok().body(errorResponse);
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
